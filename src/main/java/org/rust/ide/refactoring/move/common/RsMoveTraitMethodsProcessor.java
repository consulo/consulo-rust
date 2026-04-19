/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.ide.refactoring.move.common;

import org.rust.ide.refactoring.move.common.RsMoveUtil;
import com.intellij.openapi.util.Key;
import com.intellij.openapi.util.NlsContexts.DialogMessage;
import com.intellij.psi.PsiElement;
import com.intellij.util.containers.MultiMap;
import com.intellij.openapi.util.Pair;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.rust.ide.utils.imports.RsImportHelper;
import org.rust.lang.core.psi.*;
import org.rust.lang.core.psi.ext.*;

import java.util.*;
import java.util.stream.Collectors;
import org.rust.lang.core.psi.ext.RsTraitRefUtil;
import org.rust.lang.core.psi.ext.RsMod;

public class RsMoveTraitMethodsProcessor {

    private static final Key<Pair<RsTraitItem, String>> RS_METHOD_CALL_TRAIT_USE_PATH = Key.create("RS_METHOD_CALL_TRAIT_USE_PATH");

    @NotNull
    private final RsPsiFactory psiFactory;
    @NotNull
    private final RsMod sourceMod;
    @NotNull
    private final RsMod targetMod;
    @NotNull
    private final RsMovePathHelper pathHelper;

    public RsMoveTraitMethodsProcessor(
        @NotNull RsPsiFactory psiFactory,
        @NotNull RsMod sourceMod,
        @NotNull RsMod targetMod,
        @NotNull RsMovePathHelper pathHelper
    ) {
        this.psiFactory = psiFactory;
        this.sourceMod = sourceMod;
        this.targetMod = targetMod;
        this.pathHelper = pathHelper;
    }

    public void preprocessOutsideReferences(
        @NotNull MultiMap<PsiElement, @DialogMessage String> conflicts,
        @NotNull List<ElementToMove> elementsToMove
    ) {
        List<RsReferenceElement> references = getReferencesToTraitAssocItems(elementsToMove);
        preprocessReferencesToTraitAssocItems(
            references,
            conflicts,
            trait -> RsImportHelper.findPath(targetMod, trait),
            trait -> !RsMoveUtil.isInsideMovedElements(trait, elementsToMove)
        );
    }

    public void preprocessInsideReferences(
        @NotNull MultiMap<PsiElement, @DialogMessage String> conflicts,
        @NotNull List<ElementToMove> elementsToMove
    ) {
        Set<RsTraitItem> traitsToMove = elementsToMove.stream()
            .filter(e -> e instanceof ItemToMove)
            .map(e -> ((ItemToMove) e).getItem())
            .filter(item -> item instanceof RsTraitItem)
            .map(item -> (RsTraitItem) item)
            .collect(Collectors.toSet());
        if (traitsToMove.isEmpty()) return;

        List<RsReferenceElement> references = RsElementUtil.descendantsOfType(sourceMod, RsReferenceElement.class)
            .stream()
            .filter(ref -> isMethodOrPath(ref) && !RsMoveUtil.isInsideMovedElements(ref, elementsToMove))
            .collect(Collectors.toList());
        preprocessReferencesToTraitAssocItems(
            references,
            conflicts,
            trait -> {
                RsPath path = pathHelper.findPathAfterMove(sourceMod, trait);
                return path != null ? path.getText() : null;
            },
            traitsToMove::contains
        );
    }

    private void preprocessReferencesToTraitAssocItems(
        @NotNull List<RsReferenceElement> references,
        @NotNull MultiMap<PsiElement, @DialogMessage String> conflicts,
        @NotNull java.util.function.Function<RsTraitItem, String> findTraitUsePath,
        @NotNull java.util.function.Predicate<RsTraitItem> shouldProcessTrait
    ) {
        for (RsReferenceElement reference : references) {
            PsiElement resolved = reference.getReference() != null ? reference.getReference().resolve() : null;
            if (!(resolved instanceof RsAbstractable)) continue;
            RsAbstractable assocItem = (RsAbstractable) resolved;
            if (!(assocItem instanceof RsFunction) && !(assocItem instanceof RsConstant)) continue;
            RsTraitItem trait = getTrait(assocItem);
            if (trait == null) continue;
            if (!shouldProcessTrait.test(trait)) continue;

            String traitUsePath = findTraitUsePath.apply(trait);
            if (traitUsePath == null) {
                RsAbstractable superItem = assocItem.getSuperItem();
                RsMoveConflictsDetector.addVisibilityConflict(conflicts, reference, superItem != null ? superItem : trait);
            } else {
                reference.putCopyableUserData(RS_METHOD_CALL_TRAIT_USE_PATH, new Pair<>(trait, traitUsePath));
            }
        }
    }

    public void addTraitImportsForOutsideReferences(@NotNull List<ElementToMove> elementsToMove) {
        addTraitImportsForReferences(getReferencesToTraitAssocItems(elementsToMove));
    }

    public void addTraitImportsForInsideReferences() {
        List<RsReferenceElement> references = RsElementUtil.descendantsOfType(sourceMod, RsReferenceElement.class)
            .stream()
            .filter(RsMoveTraitMethodsProcessor::isMethodOrPath)
            .collect(Collectors.toList());
        addTraitImportsForReferences(references);
    }

    private void addTraitImportsForReferences(@NotNull Collection<RsReferenceElement> references) {
        for (RsReferenceElement reference : references) {
            Pair<RsTraitItem, String> data = reference.getCopyableUserData(RS_METHOD_CALL_TRAIT_USE_PATH);
            if (data == null) continue;
            RsTraitItem trait = data.getFirst();
            String traitUsePath = data.getSecond();
            List<RsTraitItem> traitList = Collections.singletonList(trait);
            if (RsElementUtil.filterInScope(traitList, reference).isEmpty()) {
                continue;
            }
            // If trait is already in scope (after move), no import needed
            // but the original check was if filterInScope is non-empty -> skip
            // Correcting: if filterInScope is NOT empty -> trait IS in scope -> skip
            if (!RsElementUtil.filterInScope(traitList, reference).isEmpty()) continue;
            RsMoveUtil.addImport(psiFactory, reference, traitUsePath, null);
        }
    }

    @NotNull
    private static List<RsReferenceElement> getReferencesToTraitAssocItems(@NotNull List<ElementToMove> elementsToMove) {
        return RsMoveUtil.movedElementsShallowDescendantsOfType(elementsToMove, RsReferenceElement.class, false)
            .stream()
            .filter(RsMoveTraitMethodsProcessor::isMethodOrPath)
            .collect(Collectors.toList());
    }

    private static boolean isMethodOrPath(@NotNull RsReferenceElement element) {
        return element instanceof RsMethodCall || element instanceof RsPath;
    }

    @Nullable
    private static RsTraitItem getTrait(@NotNull RsAbstractable abstractable) {
        RsAbstractableOwner owner = RsAbstractableUtil.getOwner(abstractable);
        if (owner instanceof RsAbstractableOwner.Trait) {
            return ((RsAbstractableOwner.Trait) owner).getTrait();
        }
        if (owner instanceof RsAbstractableOwner.Impl) {
            RsTraitRef traitRef = ((RsAbstractableOwner.Impl) owner).getImpl().getTraitRef();
            return traitRef != null ? RsTraitRefUtil.resolveToTrait(traitRef) : null;
        }
        return null;
    }
}
