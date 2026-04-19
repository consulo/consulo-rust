/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.ide.refactoring.move.common;

import org.rust.ide.refactoring.move.common.RsMoveUtil;
import com.intellij.openapi.util.Key;
import com.intellij.openapi.util.NlsContexts.DialogMessage;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.psi.PsiElement;
import com.intellij.refactoring.util.RefactoringUIUtil;
import com.intellij.util.containers.MultiMap;
import org.jetbrains.annotations.NotNull;
import org.rust.ide.utils.PsiUtils;
import org.rust.lang.core.macros.MacrosUtil;
import org.rust.lang.core.psi.*;
import org.rust.lang.core.psi.ext.*;

import java.util.*;
import java.util.stream.Collectors;
import org.rust.lang.core.psi.ext.RsImplItemUtil;
import org.rust.lang.core.psi.ext.RsElement;
import org.rust.lang.core.psi.ext.RsMod;
import org.rust.lang.core.psi.ext.RsQualifiedNamedElement;
import com.intellij.psi.PsiFile;

public class RsMoveConflictsDetector {

    public static final Key<RsElement> RS_ELEMENT_FOR_CHECK_INSIDE_REFERENCES_VISIBILITY =
        Key.create("RS_ELEMENT_FOR_CHECK_INSIDE_REFERENCES_VISIBILITY");

    @NotNull
    private final MultiMap<PsiElement, @DialogMessage String> conflicts;
    @NotNull
    private final List<ElementToMove> elementsToMove;
    @NotNull
    private final RsMod sourceMod;
    @NotNull
    private final RsMod targetMod;
    @NotNull
    private final Set<RsElement> itemsToMakePublic = new HashSet<>();

    public RsMoveConflictsDetector(
        @NotNull MultiMap<PsiElement, @DialogMessage String> conflicts,
        @NotNull List<ElementToMove> elementsToMove,
        @NotNull RsMod sourceMod,
        @NotNull RsMod targetMod
    ) {
        this.conflicts = conflicts;
        this.elementsToMove = elementsToMove;
        this.sourceMod = sourceMod;
        this.targetMod = targetMod;
    }

    @NotNull
    public Set<RsElement> getItemsToMakePublic() {
        return itemsToMakePublic;
    }

    public void detectInsideReferencesVisibilityProblems(@NotNull List<RsMoveReferenceInfo> insideReferences) {
        updateItemsToMakePublic(insideReferences);
        detectReferencesVisibilityProblems(insideReferences);
        detectPrivateFieldOrMethodInsideReferences();
    }

    private void updateItemsToMakePublic(@NotNull List<RsMoveReferenceInfo> insideReferences) {
        for (RsMoveReferenceInfo reference : insideReferences) {
            RsElement pathOld = reference.getPathOldOriginal();
            RsQualifiedNamedElement target = reference.getTarget();
            RsMod usageMod = pathOld.getContainingMod();
            boolean isSelfReference = RsMoveUtil.isInsideMovedElements(pathOld, elementsToMove);
            if (!isSelfReference && !usageMod.getSuperMods().contains(targetMod)) {
                RsElement itemToMakePublic = target instanceof RsFile
                    ? ((RsFile) target).getDeclaration() : target;
                if (itemToMakePublic != null) {
                    itemsToMakePublic.add(itemToMakePublic);
                }
            }
        }
    }

    public void detectOutsideReferencesVisibilityProblems(@NotNull List<RsMoveReferenceInfo> outsideReferences) {
        detectReferencesVisibilityProblems(outsideReferences);
        detectPrivateFieldOrMethodOutsideReferences();
    }

    private void detectReferencesVisibilityProblems(@NotNull List<RsMoveReferenceInfo> references) {
        for (RsMoveReferenceInfo reference : references) {
            RsPath pathNew = reference.getPathNewAccessible();
            if (pathNew == null || !RsMoveUtil.isTargetOfEachSubpathAccessible(pathNew)) {
                addVisibilityConflict(conflicts, reference.getPathOldOriginal(), reference.getTarget());
            }
        }
    }

    private void detectPrivateFieldOrMethodInsideReferences() {
        List<RsElement> movedElementsShallowDescendants = RsMoveUtil.movedElementsShallowDescendantsOfType(elementsToMove, RsElement.class, true);
        com.intellij.psi.PsiFile sourceFile = sourceMod.getContainingFile();
        List<RsElement> allDescendants = RsElementUtil.descendantsOfType(sourceFile, RsElement.class);
        Set<RsElement> movedSet = new HashSet<>(movedElementsShallowDescendants);
        List<RsElement> elementsToCheck = allDescendants.stream().filter(e -> !movedSet.contains(e)).collect(Collectors.toList());
        detectPrivateFieldOrMethodReferences(elementsToCheck.stream().collect(Collectors.toList()), this::checkVisibilityForInsideReference);
    }

    private void checkVisibilityForInsideReference(@NotNull RsElement referenceElement, @NotNull RsVisible target) {
        if (target.getVisibility() == RsVisibility.Public.INSTANCE) return;
        RsMod targetContainingMod = RsElementUtil.ancestorStrict(target, RsMod.class);
        if (targetContainingMod == null) return;
        if (targetContainingMod != sourceMod) return;
        PsiElement item = PsiUtils.getTopmostParentInside(target, targetContainingMod);
        if (elementsToMove.stream().noneMatch(e -> e instanceof ItemToMove && ((ItemToMove) e).getItem() == item)) return;

        RsPsiFactory factory = new RsPsiFactory(sourceMod.getProject());
        RsModItem tempMod = factory.createModItem(RsMoveUtil.TMP_MOD_NAME, "");
        MacrosUtil.setContext(tempMod, targetMod);

        target.putCopyableUserData(RS_ELEMENT_FOR_CHECK_INSIDE_REFERENCES_VISIBILITY, (RsElement) target);
        PsiElement itemInTempMod = RsMoveUtil.addInner(tempMod, item);
        RsVisible targetInTempMod = null;
        for (RsVisible v : RsElementUtil.descendantsOfType(itemInTempMod, RsVisible.class)) {
            if (v.getCopyableUserData(RS_ELEMENT_FOR_CHECK_INSIDE_REFERENCES_VISIBILITY) == (RsElement) target) {
                targetInTempMod = v;
                break;
            }
        }
        if (targetInTempMod == null) return;

        if (!targetInTempMod.isVisibleFrom(referenceElement.getContainingMod())) {
            addVisibilityConflict(conflicts, referenceElement, (RsElement) target);
        }
        target.putCopyableUserData(RS_ELEMENT_FOR_CHECK_INSIDE_REFERENCES_VISIBILITY, null);
    }

    private void detectPrivateFieldOrMethodOutsideReferences() {
        List<RsElement> elementsToCheck = RsMoveUtil.movedElementsDeepDescendantsOfType(elementsToMove, RsElement.class);
        detectPrivateFieldOrMethodReferences(elementsToCheck, (referenceElement, target) -> {
            if (!RsMoveUtil.isInsideMovedElements(target, elementsToMove) && !target.isVisibleFrom(targetMod)) {
                addVisibilityConflict(conflicts, referenceElement, (RsElement) target);
            }
        });
    }

    private void detectPrivateFieldOrMethodReferences(
        @NotNull List<RsElement> elementsToCheck,
        @NotNull java.util.function.BiConsumer<RsElement, RsVisible> checkVisibility
    ) {
        for (RsElement element : elementsToCheck) {
            if (element instanceof RsDotExpr) {
                RsDotExpr dotExpr = (RsDotExpr) element;
                RsReferenceElement fieldReference = dotExpr.getFieldLookup();
                if (fieldReference == null) fieldReference = dotExpr.getMethodCall();
                if (fieldReference != null) {
                    PsiElement resolved = fieldReference.getReference() != null ? fieldReference.getReference().resolve() : null;
                    if (resolved instanceof RsVisible) {
                        checkVisibility.accept(fieldReference, (RsVisible) resolved);
                    }
                }
            } else if (element instanceof RsStructLiteralField) {
                RsStructLiteralField field = (RsStructLiteralField) element;
                RsElement decl = org.rust.lang.core.psi.ext.RsStructLiteralFieldUtil.resolveToDeclaration(field);
                if (decl instanceof RsVisible) {
                    checkVisibility.accept(element, (RsVisible) decl);
                }
            } else if (element instanceof RsPatField) {
                RsPatBinding patBinding = ((RsPatField) element).getPatBinding();
                if (patBinding != null) {
                    PsiElement resolved = patBinding.getReference() != null ? patBinding.getReference().resolve() : null;
                    if (resolved instanceof RsVisible) {
                        checkVisibility.accept(patBinding, (RsVisible) resolved);
                    }
                }
            } else if (element instanceof RsPatTupleStruct) {
                PsiElement resolved = ((RsPatTupleStruct) element).getPath().getReference() != null
                    ? ((RsPatTupleStruct) element).getPath().getReference().resolve() : null;
                if (resolved instanceof RsStructItem) {
                    RsStructItem struct = (RsStructItem) resolved;
                    if (struct.getTupleFields() != null) {
                        for (RsTupleFieldDecl field : struct.getTupleFields().getTupleFieldDeclList()) {
                            checkVisibility.accept(element, field);
                        }
                    }
                }
            } else if (element instanceof RsPath) {
                RsPath path = (RsPath) element;
                boolean isInsideSimplePath = false;
                PsiElement ancestor = element;
                while (ancestor instanceof RsPath) {
                    if (RsMoveUtil.isSimplePath((RsPath) ancestor)) {
                        isInsideSimplePath = true;
                        break;
                    }
                    ancestor = ancestor.getParent();
                }
                if (!isInsideSimplePath && !RsMoveUtil.startsWithSelf(path)) {
                    PsiElement resolved = path.getReference() != null ? path.getReference().resolve() : null;
                    if (resolved instanceof RsVisible) {
                        checkVisibility.accept(path, (RsVisible) resolved);
                    }
                }
            }
        }
    }

    public void checkImpls() {
        if (sourceMod.getCrateRoot() == targetMod.getCrateRoot()) return;

        Set<RsStructOrEnumItemElement> structsToMove = new HashSet<>();
        for (RsStructOrEnumItemElement s : RsMoveUtil.movedElementsDeepDescendantsOfType(elementsToMove, RsStructOrEnumItemElement.class)) {
            structsToMove.add(s);
        }
        List<RsImplItem> implsToMove = RsMoveUtil.movedElementsDeepDescendantsOfType(elementsToMove, RsImplItem.class);
        Set<RsImplItem> inherentImplsToMove = new HashSet<>();
        Set<RsImplItem> traitImplsToMove = new HashSet<>();
        for (RsImplItem impl : implsToMove) {
            if (impl.getTraitRef() == null) {
                inherentImplsToMove.add(impl);
            } else {
                traitImplsToMove.add(impl);
            }
        }

        checkStructIsMovedTogetherWithInherentImpl(structsToMove, inherentImplsToMove);
        checkInherentImplIsMovedTogetherWithStruct(structsToMove, inherentImplsToMove);
        for (RsImplItem impl : traitImplsToMove) {
            checkTraitImplIsCoherentAfterMove(impl);
        }
    }

    private void checkTraitImplIsCoherentAfterMove(@NotNull RsImplItem impl) {
        if (!RsMoveUtil.checkOrphanRules(impl, element ->
            element.getCrateRoot() == targetMod.getCrateRoot() || RsMoveUtil.isInsideMovedElements(element, elementsToMove)
        )) {
            conflicts.putValue(impl, "Orphan rules check failed for trait implementation after move");
        }
    }

    private void checkStructIsMovedTogetherWithInherentImpl(
        @NotNull Set<RsStructOrEnumItemElement> structsToMove,
        @NotNull Set<RsImplItem> inherentImplsToMove
    ) {
        for (RsImplItem impl : inherentImplsToMove) {
            RsStructOrEnumItemElement struct = RsImplItemUtil.getImplementingType(impl) != null
                ? RsImplItemUtil.getImplementingType(impl).getItem() : null;
            if (struct != null && !structsToMove.contains(struct)) {
                String structDescription = RefactoringUIUtil.getDescription(struct, true);
                conflicts.putValue(impl, "Inherent implementation should be moved together with " + structDescription);
            }
        }
    }

    private void checkInherentImplIsMovedTogetherWithStruct(
        @NotNull Set<RsStructOrEnumItemElement> structsToMove,
        @NotNull Set<RsImplItem> inherentImplsToMove
    ) {
        Map<com.intellij.psi.PsiFile, List<RsStructOrEnumItemElement>> groupedByFile = structsToMove.stream()
            .collect(Collectors.groupingBy(PsiElement::getContainingFile));

        for (Map.Entry<com.intellij.psi.PsiFile, List<RsStructOrEnumItemElement>> entry : groupedByFile.entrySet()) {
            List<RsImplItem> fileImpls = RsElementUtil.descendantsOfType(entry.getKey(), RsImplItem.class);
            Map<Object, List<RsImplItem>> structInherentImpls = fileImpls.stream()
                .filter(impl -> impl.getTraitRef() == null)
                .collect(Collectors.groupingBy(impl -> {
                    Object type = RsImplItemUtil.getImplementingType(impl);
                    return type != null ? type : new Object();
                }));
            for (RsStructOrEnumItemElement struct : entry.getValue()) {
                List<RsImplItem> impls = structInherentImpls.get(struct);
                if (impls == null) continue;
                for (RsImplItem impl : impls) {
                    if (!inherentImplsToMove.contains(impl)) {
                        String structDescription = RefactoringUIUtil.getDescription(struct, true);
                        conflicts.putValue(impl, "Inherent implementation should be moved together with " + structDescription);
                    }
                }
            }
        }
    }

    public static void addVisibilityConflict(
        @NotNull MultiMap<PsiElement, @DialogMessage String> conflicts,
        @NotNull RsElement reference,
        @NotNull RsElement target
    ) {
        String referenceDescription = RefactoringUIUtil.getDescription(reference.getContainingMod(), true);
        String targetDescription = RefactoringUIUtil.getDescription(target, true);
        String message = referenceDescription + " uses " + targetDescription + " which will be inaccessible after move";
        conflicts.putValue(reference, StringUtil.capitalize(message));
    }
}
