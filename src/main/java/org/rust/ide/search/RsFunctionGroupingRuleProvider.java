/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.ide.search;

import com.intellij.openapi.project.Project;
import com.intellij.psi.PsiElement;
import com.intellij.psi.util.PsiTreeUtil;
import com.intellij.usages.PsiNamedElementUsageGroupBase;
import com.intellij.usages.Usage;
import com.intellij.usages.UsageGroup;
import com.intellij.usages.UsageTarget;
import com.intellij.usages.impl.FileStructureGroupRuleProvider;
import com.intellij.usages.rules.PsiElementUsage;
import com.intellij.usages.rules.SingleParentUsageGroupingRule;
import com.intellij.usages.rules.UsageGroupingRule;
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
