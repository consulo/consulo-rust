/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.lang.core.psi.impl;

import consulo.document.util.TextRange;
import consulo.language.psi.LiteralTextEscaper;
import consulo.language.psi.PsiLanguageInjectionHost;
import jakarta.annotation.Nonnull;
import org.rust.lang.core.psi.*;

/**
 * Same as {@link consulo.language.psi.LiteralTextEscaper#createSimple}, but multi line
 */
public class SimpleMultiLineTextEscaper<T extends PsiLanguageInjectionHost> extends LiteralTextEscaper<T> {

    public SimpleMultiLineTextEscaper(@Nonnull T host) {
        super(host);
    }

    @Override
    public boolean decode(@Nonnull TextRange rangeInsideHost, @Nonnull StringBuilder outChars) {
        outChars.append(rangeInsideHost.substring(myHost.getText()));
        return true;
    }

    @Override
    public int getOffsetInHost(int offsetInDecoded, @Nonnull TextRange rangeInsideHost) {
        return rangeInsideHost.getStartOffset() + offsetInDecoded;
    }

    @Override
    public boolean isOneLine() {
        return false;
    }
}
