/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.lang.core.psi;

import com.intellij.openapi.util.TextRange;
import com.intellij.psi.LiteralTextEscaper;
import com.intellij.psi.PsiLanguageInjectionHost;
import org.jetbrains.annotations.NotNull;

/**
 * Same as {@link com.intellij.psi.LiteralTextEscaper#createSimple}, but multi line
 */
public class SimpleMultiLineTextEscaper<T extends PsiLanguageInjectionHost> extends LiteralTextEscaper<T> {

    public SimpleMultiLineTextEscaper(@NotNull T host) {
        super(host);
    }

    @Override
    public boolean decode(@NotNull TextRange rangeInsideHost, @NotNull StringBuilder outChars) {
        outChars.append(rangeInsideHost.substring(myHost.getText()));
        return true;
    }

    @Override
    public int getOffsetInHost(int offsetInDecoded, @NotNull TextRange rangeInsideHost) {
        return rangeInsideHost.getStartOffset() + offsetInDecoded;
    }

    @Override
    public boolean isOneLine() {
        return false;
    }
}
