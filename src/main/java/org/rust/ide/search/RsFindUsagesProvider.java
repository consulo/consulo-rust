/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.ide.search;

import com.intellij.lang.HelpID;
import com.intellij.lang.cacheBuilder.WordsScanner;
import com.intellij.lang.findUsages.FindUsagesProvider;
import com.intellij.psi.PsiElement;
import org.rust.lang.core.psi.ext.RsNamedElement;

public class RsFindUsagesProvider implements FindUsagesProvider {

    // Must return new instance of WordScanner here, because it is not thread safe
    @Override
    public WordsScanner getWordsScanner() {
        return new RsWordScanner();
    }

    @Override
    public boolean canFindUsagesFor(PsiElement element) {
        return element instanceof RsNamedElement;
    }

    @Override
    public String getHelpId(PsiElement element) {
        return HelpID.FIND_OTHER_USAGES;
    }

    @Override
    public String getType(PsiElement element) {
        return "";
    }

    @Override
    public String getDescriptiveName(PsiElement element) {
        if (element instanceof RsNamedElement) {
            String name = ((RsNamedElement) element).getName();
            return name != null ? name : "";
        }
        return "";
    }

    @Override
    public String getNodeText(PsiElement element, boolean useFullName) {
        return "";
    }
}
