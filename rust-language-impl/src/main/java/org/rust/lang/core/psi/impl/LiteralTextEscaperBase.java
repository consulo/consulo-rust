/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.lang.core.psi.impl;

import consulo.document.util.TextRange;
import consulo.language.psi.LiteralTextEscaper;
import consulo.language.psi.PsiLanguageInjectionHost;
import consulo.util.lang.Pair;
import jakarta.annotation.Nonnull;
import org.rust.lang.core.psi.*;

public abstract class LiteralTextEscaperBase<T extends PsiLanguageInjectionHost> extends LiteralTextEscaper<T> {

    private int[] outSourceOffsets;

    public LiteralTextEscaperBase(@Nonnull T host) {
        super(host);
    }

    @Override
    public boolean decode(@Nonnull TextRange rangeInsideHost, @Nonnull StringBuilder outChars) {
        String subText = rangeInsideHost.substring(myHost.getText());
        Pair<int[], Boolean> result = parseStringCharacters(subText, outChars);
        outSourceOffsets = result.getFirst();
        return result.getSecond();
    }

    @Override
    public int getOffsetInHost(int offsetInDecoded, @Nonnull TextRange rangeInsideHost) {
        int[] offsets = outSourceOffsets;
        assert offsets != null;
        int result = (offsetInDecoded < offsets.length) ? offsets[offsetInDecoded] : -1;
        if (result == -1) {
            return -1;
        } else {
            return (result <= rangeInsideHost.getLength() ? result : rangeInsideHost.getLength()) + rangeInsideHost.getStartOffset();
        }
    }

    @Nonnull
    protected abstract Pair<int[], Boolean> parseStringCharacters(@Nonnull String chars, @Nonnull StringBuilder outChars);
}
