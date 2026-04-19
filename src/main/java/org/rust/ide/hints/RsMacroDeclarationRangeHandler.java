/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.ide.hints;

import com.intellij.codeInsight.hint.DeclarationRangeHandler;
import com.intellij.openapi.util.TextRange;
import com.intellij.psi.PsiElement;
import org.jetbrains.annotations.NotNull;
import org.rust.lang.core.psi.RsMacro;

public class RsMacroDeclarationRangeHandler implements DeclarationRangeHandler<RsMacro> {
    @NotNull
    @Override
    public TextRange getDeclarationRange(@NotNull RsMacro container) {
        PsiElement identifier = container.getIdentifier();
        if (identifier != null) {
            return identifier.getTextRange();
        }
        return container.getTextRange();
    }
}
