/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.ide.search;

import com.intellij.find.findUsages.FindUsagesHandler;
import com.intellij.psi.PsiElement;

import java.util.List;

public class RsFindUsagesHandler extends FindUsagesHandler {

    private final List<PsiElement> mySecondaryElements;

    public RsFindUsagesHandler(PsiElement psiElement, List<PsiElement> secondaryElements) {
        super(psiElement);
        mySecondaryElements = secondaryElements;
    }

    @Override
    public PsiElement[] getSecondaryElements() {
        return mySecondaryElements.toArray(PsiElement.EMPTY_ARRAY);
    }
}
