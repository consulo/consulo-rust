/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.ide.search;

import com.intellij.psi.PsiElement;
import com.intellij.psi.util.PsiTreeUtil;
import com.intellij.usages.Usage;
import com.intellij.usages.UsageTarget;
import com.intellij.usages.rules.ImportFilteringRule;
import com.intellij.usages.rules.PsiElementUsage;
import org.rust.lang.core.psi.RsUseSpeck;
import org.rust.lang.core.psi.ext.RsElement;

public class RsImportFilteringRule extends ImportFilteringRule {

    @Override
    public boolean isVisible(Usage usage, UsageTarget[] targets) {
        if (!(usage instanceof PsiElementUsage)) return true;
        PsiElement element = ((PsiElementUsage) usage).getElement();
        if (!(element instanceof RsElement)) return true;
        RsUseSpeck useSpeck = PsiTreeUtil.getParentOfType(element, RsUseSpeck.class, true);
        return !(useSpeck != null && useSpeck.getAlias() == null);
    }
}
