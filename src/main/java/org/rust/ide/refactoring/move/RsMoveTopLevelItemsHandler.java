/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.ide.refactoring.move;

import com.intellij.lang.Language;
import com.intellij.openapi.actionSystem.DataContext;
import com.intellij.openapi.editor.CaretModel;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.editor.SelectionModel;
import com.intellij.openapi.project.Project;
import com.intellij.psi.*;
import com.intellij.psi.util.PsiTreeUtil;
import com.intellij.refactoring.move.MoveCallback;
import com.intellij.refactoring.move.MoveHandlerDelegate;
import com.intellij.refactoring.util.CommonRefactoringUtil;
import com.intellij.openapi.util.Pair;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
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
import org.rust.lang.core.psi.ext.RsPsiJavaUtil;
import org.rust.lang.core.psi.ext.RsElement;
import org.rust.lang.core.psi.ext.RsMod;
import com.intellij.psi.PsiFile;

public class RsMoveTopLevelItemsHandler extends MoveHandlerDelegate {

    @Override
    public boolean supportsLanguage(@NotNull Language language) {
        return language.is(RsLanguage.INSTANCE);
    }

    @Override
    public boolean canMove(
        @NotNull PsiElement @NotNull [] elements,
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
        @NotNull Project project,
        @NotNull PsiElement @NotNull [] elements,
        @Nullable PsiElement targetContainer,
        @Nullable MoveCallback moveCallback
    ) {
        doMove(project, Arrays.asList(elements), null);
    }

    @Override
    public boolean tryToMove(
        @NotNull PsiElement element,
        @NotNull Project project,
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

    private boolean doMove(@NotNull Project project, @NotNull List<PsiElement> elements, @Nullable Editor editor) {
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

    private void doMoveInUnitTestMode(@NotNull Project project, @NotNull Set<RsItemElement> itemsToMove, @NotNull RsMod sourceMod) {
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
    private Object[] collectInitialItems(@NotNull Project project, @NotNull Editor editor) {
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
    private Object[] collectItemsInsideSelection(@NotNull PsiFile file, @NotNull SelectionModel selection) {
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
    private Object[] collectItemsUnderCaret(@NotNull PsiFile file, @NotNull CaretModel caretModel) {
        List<RsItemElement> elements = new ArrayList<>();
        for (com.intellij.openapi.editor.Caret caret : caretModel.getAllCarets()) {
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

    public static boolean canMoveElement(@NotNull PsiElement element) {
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
    private static <T extends RsElement> T findCommonAncestorStrictOfType(@NotNull List<? extends PsiElement> elements, @NotNull Class<T> cls) {
        PsiElement parent = PsiTreeUtil.findCommonParent(elements.toArray(PsiElement.EMPTY_ARRAY));
        if (parent == null) return null;
        if (elements.contains(parent)) {
            return RsElementUtil.ancestorStrict(parent, cls);
        } else {
            return RsElementUtil.ancestorOrSelf(parent, cls);
        }
    }

    @NotNull
    private static List<RsImplItem> collectRelatedImplItems(@NotNull RsMod containingMod, @NotNull Set<RsItemElement> items) {
        if (org.rust.openapiext.OpenApiUtil.isUnitTestMode()) return Collections.emptyList();
        Map<RsItemElement, List<RsImplItem>> grouped = groupImplsByStructOrTrait(containingMod, items);
        return grouped.values().stream().flatMap(List::stream).collect(Collectors.toList());
    }

    @NotNull
    public static Map<RsItemElement, List<RsImplItem>> groupImplsByStructOrTrait(@NotNull RsMod containingMod, @NotNull Set<RsItemElement> items) {
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
