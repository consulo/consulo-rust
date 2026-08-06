/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.ide.hints;

import consulo.language.editor.ImplementationTextSelectioner;
import consulo.language.psi.PsiElement;
import jakarta.annotation.Nonnull;
import org.rust.lang.core.psi.RsPat;
import org.rust.lang.core.psi.RsPatBinding;

public class RsImplementationTextSelectioner implements ImplementationTextSelectioner {
    @jakarta.annotation.Nonnull @Override public consulo.language.Language getLanguage() { return org.rust.lang.RsLanguage.INSTANCE; }


    @Override
    public int getTextEndOffset(@Nonnull PsiElement element) {
        return getDefinitionRoot(element).getTextRange().getEndOffset();
    }

    @Override
    public int getTextStartOffset(@Nonnull PsiElement element) {
        return getDefinitionRoot(element).getTextRange().getStartOffset();
    }

    @Nonnull
    private static PsiElement getDefinitionRoot(@Nonnull PsiElement element) {
        PsiElement current = element;
        while (current instanceof RsPatBinding || current instanceof RsPat) {
            current = current.getParent();
        }
        return current;
    }
}
