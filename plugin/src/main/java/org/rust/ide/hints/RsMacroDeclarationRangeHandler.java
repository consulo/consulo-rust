/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.ide.hints;

import consulo.annotation.component.ExtensionImpl;
import consulo.language.editor.hint.DeclarationRangeHandler;
import consulo.document.util.TextRange;
import consulo.language.psi.PsiElement;
import jakarta.annotation.Nonnull;
import org.rust.lang.core.psi.RsMacro;

@ExtensionImpl
public class RsMacroDeclarationRangeHandler implements DeclarationRangeHandler<RsMacro> {

    @Override
    public Class<RsMacro> getElementClass() {
        return RsMacro.class;
    }

    @Nonnull
    @Override
    public TextRange getDeclarationRange(@Nonnull RsMacro container) {
        PsiElement identifier = container.getIdentifier();
        if (identifier != null) {
            return identifier.getTextRange();
        }
        return container.getTextRange();
    }
}
