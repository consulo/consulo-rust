/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.ide.refactoring.move;
import consulo.language.psi.PsiDocumentManager;

import consulo.language.Language;
import consulo.dataContext.DataContext;
import consulo.codeEditor.CaretModel;
import consulo.codeEditor.Editor;
import consulo.codeEditor.SelectionModel;
import consulo.project.Project;
import consulo.language.psi.PsiElement;
import consulo.language.psi.PsiFile;
import consulo.language.psi.PsiReference;
import consulo.language.psi.PsiManager;
import consulo.language.psi.PsiWhiteSpace;
import consulo.language.psi.PsiComment;
import consulo.language.psi.PsiNamedElement;
import consulo.language.psi.PsiNameIdentifierOwner;
import consulo.language.psi.PsiDirectory;
import consulo.language.psi.PsiErrorElement;
import consulo.language.psi.PsiElementVisitor;
import consulo.language.psi.PsiElementResolveResult;
import consulo.language.psi.ResolveResult;
import consulo.language.psi.PsiPolyVariantReference;
import consulo.language.psi.PsiReferenceBase;
import consulo.language.psi.PsiUtilCore;
import consulo.language.psi.StubBasedPsiElement;
import consulo.language.psi.SmartPsiElementPointer;
import consulo.language.psi.ElementManipulators;
import consulo.language.psi.ElementManipulator;
import consulo.language.psi.LiteralTextEscaper;
import consulo.language.psi.NavigatablePsiElement;
import consulo.language.psi.PsiLanguageInjectionHost;
import consulo.language.psi.ContributedReferenceHost;
import consulo.language.psi.SyntaxTraverser;
import consulo.language.psi.util.PsiTreeUtil;
import consulo.language.editor.refactoring.move.MoveCallback;
import consulo.language.editor.refactoring.move.MoveHandlerDelegate;
import consulo.language.editor.refactoring.util.CommonRefactoringUtil;
import consulo.util.lang.Pair;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import org.rust.ide.utils.PsiUtils;
import org.rust.lang.RsLanguage;
import org.rust.lang.core.psi.*;
import org.rust.lang.core.psi.ext.*;
import org.rust.lang.core.types.RsTypesUtil;
import org.rust.lang.core.types.ty.TyAdt;
import org.rust.openapiext.OpenApiUtil;
import org.rust.openapiext.VirtualFileExtUtil;

import java.nio.file.Path;
import java.util.*;
import java.util.stream.Collectors;
import org.rust.lang.core.psi.ext.impl.RsPsiJavaUtil;
import org.rust.lang.core.psi.ext.RsElement;
import org.rust.lang.core.psi.ext.RsMod;
import consulo.language.psi.PsiFile;
import consulo.annotation.component.ExtensionImpl;
import consulo.codeEditor.Caret;
import org.rust.lang.core.psi.ext.impl.*;

@ExtensionImpl(id = "rust.moveTopLevelItems", order = "first, before moveJavaFileOrDir, before moveFileOrDir, before rust.moveFilesOrDirectories")
public class RsMoveTopLevelItemsHandler extends MoveHandlerDelegate {

    public boolean supportsLanguage(@Nonnull Language language) {
        return language.is(RsLanguage.INSTANCE);
    }

    public boolean canMove(
        @Nonnull PsiElement [] elements,
        @Nullable PsiElement targetContainer,
        @Nullable PsiReference reference
    ) {
        if (elements.length == 0) return false;
        PsiElement first = elements[0];
        if (!(first instanceof RsElement)) return false;
        RsMod containingMod = ((RsElement) first).getContainingMod();
        if (containingMod == null) return false;
        for (PsiElement element : elements) {
            if (!canMoveElement(element) || element.getParent() != containingMod) return false;
        }
        return true;
    }

    @Override
    public void doMove(
        @Nonnull Project project,
        @Nonnull PsiElement [] elements,
        @Nullable PsiElement targetContainer,
        @Nullable MoveCallback moveCallback
    ) {
        doMove(project, Arrays.asList(elements), null);
    }

    @Override
    public boolean tryToMove(
        @Nonnull PsiElement element,
        @Nonnull Project project,
        @Nullable DataContext dataContext,
        @Nullable PsiReference reference,
        @Nullable Editor editor
    ) {
        boolean hasSelection = editor != null && editor.getSelectionModel().hasSelection();
        if (hasSelection || !(element instanceof PsiFile)) {
            return doMove(project, Collections.singletonList(element), editor);
        }
        return false;
    }

    private boolean doMove(@Nonnull Project project, @Nonnull List<PsiElement> elements, @Nullable Editor editor) {
        Set<RsItemElement> itemsToMove;
        RsMod containingMod;

        if (editor != null) {
            Object[] result = collectInitialItems(project, editor);
            if (result == null) return false;
            itemsToMove = (Set<RsItemElement>) result[0];
            containingMod = (RsMod) result[1];
        } else {
            containingMod = findCommonAncestorStrictOfType(elements, RsMod.class);
            if (containingMod == null) return false;
            itemsToMove = elements.stream()
                .filter(e -> e instanceof RsItemElement)
                .map(e -> (RsItemElement) e)
                .collect(Collectors.toCollection(LinkedHashSet::new));
        }

        if (!CommonRefactoringUtil.checkReadOnlyStatusRecursively(project, itemsToMove, true)) return false;

        List<RsImplItem> relatedImplItems = collectRelatedImplItems(containingMod, itemsToMove);
        Set<RsItemElement> itemsToMoveAll = new LinkedHashSet<>(itemsToMove);
        itemsToMoveAll.addAll(relatedImplItems);

        if (org.rust.openapiext.OpenApiUtil.isUnitTestMode()) {
            doMoveInUnitTestMode(project, itemsToMoveAll, containingMod);
        } else {
            new RsMoveTopLevelItemsDialog(project, itemsToMoveAll, containingMod).show();
        }
        return true;
    }

    private void doMoveInUnitTestMode(@Nonnull Project project, @Nonnull Set<RsItemElement> itemsToMove, @Nonnull RsMod sourceMod) {
        PsiFile sourceFile = sourceMod.getContainingFile();
        RsMod targetMod = sourceFile.getUserData(RsMoveTopLevelItemsDialog.MOVE_TARGET_MOD_KEY);
        if (targetMod == null) {
            Path targetPath = sourceFile.getUserData(RsMoveTopLevelItemsDialog.MOVE_TARGET_FILE_PATH_KEY);
            targetMod = RsMoveTopLevelItemsDialog.getOrCreateTargetMod(targetPath, project, sourceMod.getCrateRoot());
        }

        RsMoveTopLevelItemsProcessor processor = new RsMoveTopLevelItemsProcessor(project, itemsToMove, targetMod, true);
        processor.run();
    }

    @Nullable
    private Object[] collectInitialItems(@Nonnull Project project, @Nonnull Editor editor) {
        PsiFile file = PsiDocumentManager.getInstance(project).getPsiFile(editor.getDocument());
        if (file == null) return null;
        SelectionModel selection = editor.getSelectionModel();
        if (selection.hasSelection()) {
            return collectItemsInsideSelection(file, selection);
        } else {
            return collectItemsUnderCaret(file, editor.getCaretModel());
        }
    }

    @Nullable
    private Object[] collectItemsInsideSelection(@Nonnull PsiFile file, @Nonnull SelectionModel selection) {
        PsiElement[] range = PsiUtils.getElementRange(file, selection.getSelectionStart(), selection.getSelectionEnd());
        if (range == null) return null;
        PsiElement leafElement1 = range[0];
        PsiElement leafElement2 = range[1];
        RsElement element1 = RsElementUtil.ancestorOrSelf(leafElement1, RsElement.class);
        RsElement element2 = RsElementUtil.ancestorOrSelf(leafElement2, RsElement.class);
        if (element1 == null || element2 == null) return null;

        RsMod containingMod = findCommonAncestorStrictOfType(Arrays.asList(element1, element2), RsMod.class);
        if (containingMod == null) return null;

        PsiElement item1 = PsiUtils.getTopmostParentInside(element1, containingMod);
        PsiElement item2 = PsiUtils.getTopmostParentInside(element2, containingMod);
        Set<RsItemElement> items = new LinkedHashSet<>();
        PsiElement current = item1;
        while (current != null && current != item2.getNextSibling()) {
            if (current instanceof RsItemElement) {
                items.add((RsItemElement) current);
            }
            current = current.getNextSibling();
        }
        return new Object[]{items, containingMod};
    }

    @Nullable
    private Object[] collectItemsUnderCaret(@Nonnull PsiFile file, @Nonnull CaretModel caretModel) {
        List<RsItemElement> elements = new ArrayList<>();
        for (consulo.codeEditor.Caret caret : caretModel.getAllCarets()) {
            int offset = caret.getOffset();
            PsiElement leafElement = file.findElementAt(offset);
            PsiElement element;
            if (offset > 0 && leafElement instanceof PsiWhiteSpace) {
                element = file.findElementAt(offset - 1);
            } else {
                element = leafElement;
            }
            RsItemElement item = element != null ? RsElementUtil.ancestorOrSelf(element, RsItemElement.class) : null;
            if (item != null) {
                elements.add(item);
            }
        }

        RsMod containingMod = findCommonAncestorStrictOfType(new ArrayList<>(elements), RsMod.class);
        if (containingMod == null) return null;

        List<RsItemElement> items = new ArrayList<>();
        for (RsItemElement element : elements) {
            PsiElement topmost = PsiUtils.getTopmostParentInside(element, containingMod);
            if (topmost instanceof RsItemElement) {
                items.add((RsItemElement) topmost);
            }
        }
        if (items.isEmpty()) return null;
        return new Object[]{new LinkedHashSet<>(items), containingMod};
    }

    public static boolean canMoveElement(@Nonnull PsiElement element) {
        if (element instanceof RsModItem && RsPsiJavaUtil.descendantOfTypeStrict((RsModItem) element, RsModDeclItem.class) != null) {
            return false;
        }
        return element instanceof RsItemElement
            && !(element instanceof RsModDeclItem)
            && !(element instanceof RsUseItem)
            && !(element instanceof RsExternCrateItem)
            && !(element instanceof RsForeignModItem);
    }

    @Nullable
    @SuppressWarnings("unchecked")
    private static <T extends RsElement> T findCommonAncestorStrictOfType(@Nonnull List<? extends PsiElement> elements, @Nonnull Class<T> cls) {
        PsiElement parent = PsiTreeUtil.findCommonParent(elements.toArray(PsiElement.EMPTY_ARRAY));
        if (parent == null) return null;
        if (elements.contains(parent)) {
            return RsElementUtil.ancestorStrict(parent, cls);
        } else {
            return RsElementUtil.ancestorOrSelf(parent, cls);
        }
    }

    @Nonnull
    private static List<RsImplItem> collectRelatedImplItems(@Nonnull RsMod containingMod, @Nonnull Set<RsItemElement> items) {
        if (org.rust.openapiext.OpenApiUtil.isUnitTestMode()) return Collections.emptyList();
        Map<RsItemElement, List<RsImplItem>> grouped = groupImplsByStructOrTrait(containingMod, items);
        return grouped.values().stream().flatMap(List::stream).collect(Collectors.toList());
    }

    @Nonnull
    public static Map<RsItemElement, List<RsImplItem>> groupImplsByStructOrTrait(@Nonnull RsMod containingMod, @Nonnull Set<RsItemElement> items) {
        Map<RsItemElement, List<RsImplItem>> result = new LinkedHashMap<>();
        for (RsImplItem impl : RsElementUtil.childrenOfType(containingMod, RsImplItem.class)) {
            RsItemElement relatedStruct = null;
            RsItemElement relatedTrait = null;

            if (impl.getTypeReference() != null) {
                Object rawType = RsTypesUtil.getRawType(impl.getTypeReference());
                if (rawType instanceof TyAdt) {
                    RsItemElement structItem = (RsItemElement) ((TyAdt) rawType).getItem();
                    if (items.contains(structItem)) {
                        relatedStruct = structItem;
                    }
                }
            }

            if (impl.getTraitRef() != null && impl.getTraitRef().getPath() != null) {
                PsiElement resolved = impl.getTraitRef().getPath().getReference() != null
                    ? impl.getTraitRef().getPath().getReference().resolve() : null;
                if (resolved instanceof RsTraitItem && items.contains(resolved)) {
                    relatedTrait = (RsItemElement) resolved;
                }
            }

            RsItemElement relatedItem = relatedStruct != null ? relatedStruct : relatedTrait;
            if (relatedItem != null) {
                result.computeIfAbsent(relatedItem, k -> new ArrayList<>()).add(impl);
            }
        }
        return result;
    }
}
