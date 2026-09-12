/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.ide.search;

import consulo.ide.impl.compiler.HelpID;
import consulo.language.cacheBuilder.WordsScanner;
import consulo.language.findUsage.FindUsagesProvider;
import consulo.language.psi.PsiElement;
import org.rust.lang.core.psi.ext.RsNamedElement;
import consulo.annotation.component.ExtensionImpl;
import consulo.language.Language;
import org.rust.lang.RsLanguage;

@ExtensionImpl
public class RsFindUsagesProvider implements FindUsagesProvider {
    @jakarta.annotation.Nonnull @Override public consulo.language.Language getLanguage() { return org.rust.lang.RsLanguage.INSTANCE; }


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
        return "find.otherUsages"; // HelpID.FIND_OTHER_USAGES not in Consulo
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
