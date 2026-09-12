/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.ide.search;

import consulo.annotation.component.ExtensionImpl;
import consulo.language.psi.PsiElement;
import consulo.language.psi.util.PsiTreeUtil;
import consulo.usage.Usage;
import consulo.usage.UsageTarget;
import consulo.usage.rule.ImportFilteringRule;
import consulo.usage.rule.PsiElementUsage;
import org.rust.lang.core.psi.RsUseSpeck;
import org.rust.lang.core.psi.ext.RsElement;

@ExtensionImpl
public class RsImportFilteringRule implements ImportFilteringRule {

    @Override
    public boolean isVisible(Usage usage, UsageTarget[] targets) {
        if (!(usage instanceof PsiElementUsage)) return true;
        PsiElement element = ((PsiElementUsage) usage).getElement();
        if (!(element instanceof RsElement)) return true;
        RsUseSpeck useSpeck = PsiTreeUtil.getParentOfType(element, RsUseSpeck.class, true);
        return !(useSpeck != null && useSpeck.getAlias() == null);
    }
}
