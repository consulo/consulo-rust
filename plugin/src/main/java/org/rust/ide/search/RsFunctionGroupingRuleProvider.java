/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.ide.search;

import consulo.project.Project;
import consulo.language.psi.PsiElement;
import consulo.language.psi.util.PsiTreeUtil;
import consulo.usage.PsiNamedElementUsageGroupBase;
import consulo.usage.Usage;
import consulo.usage.UsageGroup;
import consulo.usage.UsageTarget;
import consulo.usage.rule.FileStructureGroupRuleProvider;
import consulo.usage.rule.PsiElementUsage;
import consulo.usage.rule.SingleParentUsageGroupingRule;
import consulo.usage.rule.UsageGroupingRule;
import org.rust.lang.core.psi.RsFunction;
import org.rust.lang.core.psi.RsImplItem;

public class RsFunctionGroupingRuleProvider implements FileStructureGroupRuleProvider {

    @Override
    public UsageGroupingRule getUsageGroupingRule(Project project) {
        return new RsFunctionGroupingRule();
    }

    private static class RsFunctionGroupingRule extends SingleParentUsageGroupingRule {
        @Override
        protected UsageGroup getParentGroupFor(Usage usage, UsageTarget[] targets) {
            if (!(usage instanceof PsiElementUsage)) return null;
            PsiElement element = ((PsiElementUsage) usage).getElement();
            if (element == null) return null;
            RsFunction rsFunction = PsiTreeUtil.getParentOfType(element, RsFunction.class, false, RsImplItem.class);
            if (rsFunction == null) return null;
            return new PsiNamedElementUsageGroupBase<>(rsFunction);
        }
    }
}
