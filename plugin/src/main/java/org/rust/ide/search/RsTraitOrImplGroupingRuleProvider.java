/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.ide.search;

import consulo.annotation.component.ExtensionImpl;
import consulo.project.Project;
import consulo.language.psi.PsiElement;
import consulo.language.psi.util.PsiTreeUtil;
import consulo.usage.PsiElementUsageGroupBase;
import consulo.usage.Usage;
import consulo.usage.UsageGroup;
import consulo.usage.UsageTarget;
import consulo.usage.rule.FileStructureGroupRuleProvider;
import consulo.usage.rule.PsiElementUsage;
import consulo.usage.rule.SingleParentUsageGroupingRule;
import consulo.usage.rule.UsageGroupingRule;

import org.rust.RsBundle;
import org.rust.ide.presentation.PresentationInfo;
import org.rust.lang.core.psi.RsImplItem;
import org.rust.lang.core.psi.ext.RsTraitOrImpl;
import org.rust.ide.presentation.RsPsiRendererUtil;

@ExtensionImpl(order = "before rs-function")
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
            public String getPresentableName() {
                return myName != null ? myName : super.getPresentableName();
            }
        }
    }
}
