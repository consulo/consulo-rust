/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.ide.inspections;

import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.TextRange;
import com.intellij.psi.PsiElement;
import com.intellij.psi.tree.IElementType;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.rust.RsBundle;
import org.rust.ide.fixes.RsQuickFixBase;
import org.rust.lang.core.psi.RsImplItem;
import org.rust.lang.core.psi.RsTraitItem;
import org.rust.lang.core.psi.RsVisitor;
import org.rust.lang.core.psi.ext.*;
import org.rust.openapiext.Testmark;

import java.util.*;
import com.intellij.openapi.util.Pair;
import org.rust.lang.core.psi.ext.RsTraitRefUtil;

public class RsSortImplTraitMembersInspection extends RsLocalInspectionTool {

    @Override
    public RsVisitor buildVisitor(@NotNull RsProblemsHolder holder, boolean isOnTheFly) {
        return new RsWithMacrosInspectionVisitor() {
            @SuppressWarnings("PARAMETER_NAME_CHANGED_ON_OVERRIDE")
            @Override
            public void visitImplItem2(@NotNull RsImplItem impl) {
                if (impl.getTraitRef() == null) return;
                RsTraitItem trait = RsTraitRefUtil.resolveToTrait(impl.getTraitRef());
                if (trait == null) return;
                if (impl.getTypeReference() == null) return;
                PsiElement typeRef = impl.getTypeReference();
                List<RsAbstractable> sortedItems = sortedImplItems(
                    RsMembersUtil.getExplicitMembers(impl),
                    RsMembersUtil.getExplicitMembers(trait)
                );
                if (sortedItems == null) return;
                PsiElement firstElement = impl.getVis();
                if (firstElement == null) firstElement = RsImplItemUtil.getDefault(impl);
                if (firstElement == null) firstElement = impl.getUnsafe();
                if (firstElement == null) firstElement = impl.getImpl();
                TextRange textRange = new TextRange(
                    firstElement.getStartOffsetInParent(),
                    typeRef.getStartOffsetInParent() + typeRef.getTextLength()
                );
                holder.registerProblem(
                    impl,
                    textRange,
                    RsBundle.message("inspection.message.different.impl.member.order.from.trait"),
                    new SortImplTraitMembersFix(impl)
                );
            }
        };
    }

    @Nullable
    private static List<RsAbstractable> sortedImplItems(@NotNull List<RsAbstractable> implItems, @NotNull List<RsAbstractable> traitItems) {
        Map<Pair<String, IElementType>, Integer> traitItemMap = new HashMap<>();
        for (int i = 0; i < traitItems.size(); i++) {
            traitItemMap.put(key(traitItems.get(i)), i);
        }
        for (RsAbstractable item : implItems) {
            if (!traitItemMap.containsKey(key(item))) {
                Testmarks.ImplMemberNotInTrait.hit();
                return null;
            }
        }
        List<RsAbstractable> sorted = new ArrayList<>(implItems);
        sorted.sort(Comparator.comparingInt(item -> traitItemMap.getOrDefault(key(item), 0)));
        if (sorted.equals(implItems)) return null;
        return sorted;
    }

    private static Pair<String, IElementType> key(@NotNull RsAbstractable item) {
        return new Pair<>(item.getName(), item.getNode().getElementType());
    }

    private static class SortImplTraitMembersFix extends RsQuickFixBase<RsImplItem> {

        SortImplTraitMembersFix(@NotNull RsImplItem element) {
            super(element);
        }

        @NotNull
        @Override
        public String getText() {
            return RsBundle.message("intention.name.apply.same.member.order");
        }

        @NotNull
        @Override
        public String getFamilyName() {
            return getText();
        }

        @Override
        public void invoke(@NotNull Project project, @Nullable Editor editor, @NotNull RsImplItem element) {
            if (element.getTraitRef() == null) return;
            RsTraitItem trait = RsTraitRefUtil.resolveToTrait(element.getTraitRef());
            if (trait == null) return;
            List<RsAbstractable> implItems = RsMembersUtil.getExplicitMembers(element);
            List<RsAbstractable> traitItems = RsMembersUtil.getExplicitMembers(trait);

            List<RsAbstractable> sortedImplItemsList = sortedImplItems(implItems, traitItems);
            if (sortedImplItemsList == null) return;
            for (int index = 0; index < implItems.size(); index++) {
                RsAbstractable implItem = implItems.get(index);
                RsAbstractable expectedImplItem = sortedImplItemsList.get(index);
                if (!key(implItem).equals(key(expectedImplItem))) {
                    implItem.replace(sortedImplItemsList.get(index).copy());
                }
            }
        }
    }

    public static class Testmarks {
        public static final Testmark ImplMemberNotInTrait = new Testmark() {};
    }
}
