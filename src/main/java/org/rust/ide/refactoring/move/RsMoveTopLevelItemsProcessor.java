/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.ide.refactoring.move;

import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.NlsContexts.DialogMessage;
import com.intellij.openapi.util.Ref;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiWhiteSpace;
import com.intellij.refactoring.BaseRefactoringProcessor;
import com.intellij.refactoring.move.MoveMultipleElementsViewDescriptor;
import com.intellij.usageView.UsageInfo;
import com.intellij.usageView.UsageViewDescriptor;
import com.intellij.util.containers.MultiMap;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.rust.RsBundle;
import org.rust.ide.refactoring.move.common.ElementToMove;
import org.rust.ide.refactoring.move.common.RsMoveCommonProcessor;
import org.rust.ide.refactoring.move.common.RsMoveUtil;
import org.rust.lang.core.psi.RsModItem;
import org.rust.lang.core.psi.RsPsiFactory;
import org.rust.lang.core.psi.ext.*;
import org.rust.lang.core.resolve.RsResolveUtil;

import java.util.*;
import java.util.stream.Collectors;
import org.rust.lang.core.psi.ext.RsMod;

/**
 * See overview of move refactoring in comment for {@link RsMoveCommonProcessor}.
 */
public class RsMoveTopLevelItemsProcessor extends BaseRefactoringProcessor {

    @NotNull
    private final Project project;
    @NotNull
    private final Set<RsItemElement> itemsToMove;
    @NotNull
    private final RsMod targetMod;
    private final boolean searchForReferences;
    @NotNull
    private final RsMoveCommonProcessor commonProcessor;

    public RsMoveTopLevelItemsProcessor(
        @NotNull Project project,
        @NotNull Set<RsItemElement> itemsToMove,
        @NotNull RsMod targetMod,
        boolean searchForReferences
    ) {
        super(project);
        this.project = project;
        this.itemsToMove = itemsToMove;
        this.targetMod = targetMod;
        this.searchForReferences = searchForReferences;

        List<ElementToMove> elementsToMove = itemsToMove.stream()
            .map(ElementToMove::fromItem)
            .collect(Collectors.toList());
        this.commonProcessor = new RsMoveCommonProcessor(project, elementsToMove, targetMod);
    }

    @Override
    @NotNull
    protected UsageInfo @NotNull [] findUsages() {
        if (!searchForReferences) return UsageInfo.EMPTY_ARRAY;
        return commonProcessor.findUsages();
    }

    private void checkNoItemsWithSameName(@NotNull MultiMap<PsiElement, @DialogMessage String> conflicts) {
        if (!searchForReferences) return;

        List<RsNamedElement> targetModItems = new ArrayList<>();
        for (RsItemElement item : RsModUtil.getExpandedItemsExceptImplsAndUses(targetMod)) {
            if (item instanceof RsNamedElement) {
                targetModItems.add((RsNamedElement) item);
            }
        }
        Map<String, List<RsNamedElement>> grouped = new HashMap<>();
        for (RsNamedElement item : targetModItems) {
            if (item.getName() != null) {
                grouped.computeIfAbsent(item.getName(), k -> new ArrayList<>()).add(item);
            }
        }

        for (RsItemElement item : itemsToMove) {
            if (!(item instanceof RsNamedElement)) continue;
            String name = ((RsNamedElement) item).getName();
            if (name == null) continue;
            Set<?> namespaces = org.rust.lang.core.resolve.Namespace.getNamespaces((RsNamedElement) item);
            List<RsNamedElement> itemsExisting = grouped.get(name);
            if (itemsExisting == null) continue;
            for (RsNamedElement itemExisting : itemsExisting) {
                Set<?> namespacesExisting = org.rust.lang.core.resolve.Namespace.getNamespaces(itemExisting);
                Set<?> intersection = new HashSet<>(namespacesExisting);
                intersection.retainAll(namespaces);
                if (!intersection.isEmpty()) {
                    conflicts.putValue(itemExisting, "Target file already contains item with name " + name);
                }
            }
        }
    }

    @Override
    protected boolean preprocessUsages(@NotNull Ref<UsageInfo[]> refUsages) {
        UsageInfo[] usages = refUsages.get();
        MultiMap<PsiElement, String> conflicts = new MultiMap<>();
        checkNoItemsWithSameName(conflicts);
        return commonProcessor.preprocessUsages(usages, conflicts) && showConflicts(conflicts, usages);
    }

    @Override
    protected void performRefactoring(@NotNull UsageInfo @NotNull [] usages) {
        commonProcessor.performRefactoring(usages, this::moveItems);
    }

    @NotNull
    private List<ElementToMove> moveItems() {
        RsPsiFactory psiFactory = new RsPsiFactory(project);
        return itemsToMove.stream()
            .sorted(Comparator.comparingInt(PsiElement::getStartOffsetInParent))
            .map(item -> moveItem(item, psiFactory))
            .collect(Collectors.toList());
    }

    @NotNull
    private ElementToMove moveItem(@NotNull RsItemElement item, @NotNull RsPsiFactory psiFactory) {
        commonProcessor.updateMovedItemVisibility(item);

        PsiElement lastChildInner = getLastChildInner(targetMod);
        if (!(lastChildInner instanceof PsiWhiteSpace)) {
            RsMoveUtil.addInner(targetMod, psiFactory.createNewline());
        }
        PsiElement targetModLastWhiteSpace = getLastChildInner(targetMod);

        PsiElement space = item.getPrevSibling() instanceof PsiWhiteSpace
            ? item.getPrevSibling()
            : (item.getNextSibling() instanceof PsiWhiteSpace ? item.getNextSibling() : null);

        RsItemElement itemNew = (RsItemElement) targetMod.addBefore(item.copy(), targetModLastWhiteSpace);
        targetMod.addBefore(space != null ? space.copy() : psiFactory.createNewline(), itemNew);

        if (space != null) space.delete();
        item.delete();

        return ElementToMove.fromItem(itemNew);
    }

    @Override
    @NotNull
    protected UsageViewDescriptor createUsageViewDescriptor(@NotNull UsageInfo @NotNull [] usages) {
        String name = targetMod.getName();
        return new MoveMultipleElementsViewDescriptor(itemsToMove.toArray(PsiElement.EMPTY_ARRAY), name != null ? name : "");
    }

    @Override
    @NotNull
    protected String getCommandName() {
        return RsBundle.message("command.name.move.items");
    }

    @Nullable
    private static PsiElement getLastChildInner(@NotNull RsMod mod) {
        if (mod instanceof RsModItem) {
            PsiElement rbrace = ((RsModItem) mod).getRbrace();
            return rbrace != null ? rbrace.getPrevSibling() : null;
        }
        return mod.getLastChild();
    }
}
