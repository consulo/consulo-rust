/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.ide.hints;

import com.intellij.codeInsight.hint.ImplementationTextSelectioner;
import com.intellij.psi.PsiElement;
import org.jetbrains.annotations.NotNull;
import org.rust.lang.core.psi.RsPat;
import org.rust.lang.core.psi.RsPatBinding;

public class RsImplementationTextSelectioner implements ImplementationTextSelectioner {

    @Override
    public int getTextEndOffset(@NotNull PsiElement element) {
        return getDefinitionRoot(element).getTextRange().getEndOffset();
    }

    @Override
    public int getTextStartOffset(@NotNull PsiElement element) {
        return getDefinitionRoot(element).getTextRange().getStartOffset();
    }

    @NotNull
    private static PsiElement getDefinitionRoot(@NotNull PsiElement element) {
        PsiElement current = element;
        while (current instanceof RsPatBinding || current instanceof RsPat) {
            current = current.getParent();
        }
        return current;
    }
}
