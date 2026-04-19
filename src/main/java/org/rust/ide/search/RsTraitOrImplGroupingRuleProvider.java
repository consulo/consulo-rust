/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.ide.search;

import com.intellij.openapi.project.Project;
import com.intellij.psi.PsiElement;
import com.intellij.psi.util.PsiTreeUtil;
import com.intellij.usages.PsiElementUsageGroupBase;
import com.intellij.usages.Usage;
import com.intellij.usages.UsageGroup;
import com.intellij.usages.UsageTarget;
import com.intellij.usages.impl.FileStructureGroupRuleProvider;
import com.intellij.usages.rules.PsiElementUsage;
import com.intellij.usages.rules.SingleParentUsageGroupingRule;
import com.intellij.usages.rules.UsageGroupingRule;
import org.jetbrains.annotations.Nls;
import org.rust.RsBundle;
import org.rust.ide.presentation.PresentationInfo;
import org.rust.lang.core.psi.RsImplItem;
import org.rust.lang.core.psi.ext.RsTraitOrImpl;
import org.rust.ide.presentation.RsPsiRendererUtil;

public class RsTraitOrImplGroupingRuleProvider implements FileStructureGroupRuleProvider {

    @Override
    public UsageGroupingRule getUsageGroupingRule(Project project) {
        return new RsImplGroupingRule();
    }

    private static class RsImplGroupingRule extends SingleParentUsageGroupingRule {
        @Override
        protected UsageGroup getParentGroupFor(Usage usage, UsageTarget[] targets) {
            if (!(usage instanceof PsiElementUsage)) return null;
            PsiElement element = ((PsiElementUsage) usage).getElement();
            if (element == null) return null;
            RsTraitOrImpl traitOrImpl = PsiTreeUtil.getParentOfType(element, RsTraitOrImpl.class, false);
            if (traitOrImpl == null) return null;
            return new RsImplUsageGroup(traitOrImpl);
        }

        private static class RsImplUsageGroup extends PsiElementUsageGroupBase<RsTraitOrImpl> {
            @Nls
            private final String myName;

            RsImplUsageGroup(RsTraitOrImpl traitOrImpl) {
                super(traitOrImpl);
                myName = computeName(traitOrImpl);
            }

            private static String computeName(RsTraitOrImpl traitOrImpl) {
                if (traitOrImpl instanceof RsImplItem) {
                    RsImplItem implItem = (RsImplItem) traitOrImpl;
                    if (implItem.getTypeReference() == null) return null;
                    String type = RsPsiRendererUtil.getStubOnlyText(implItem.getTypeReference());
                    if (type == null) return null;
                    if (implItem.getTraitRef() != null) {
                        String trait = RsPsiRendererUtil.getStubOnlyText(implItem.getTraitRef());
                        if (trait != null) {
                            return RsBundle.message("0.for.1", trait, type);
                        }
                    }
                    return type;
                }
                return null;
            }

            @Override
            public String getPresentableGroupText() {
                return myName != null ? myName : super.getPresentableGroupText();
            }

            @Override
            public String getPresentableName() {
                return myName != null ? myName : super.getPresentableName();
            }
        }
    }
}
