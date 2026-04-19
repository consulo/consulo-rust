/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.lang.core.psi;

import com.intellij.openapi.util.TextRange;
import com.intellij.psi.LiteralTextEscaper;
import com.intellij.psi.PsiLanguageInjectionHost;
import com.intellij.openapi.util.Pair;
import org.jetbrains.annotations.NotNull;

/**
 * See {@code com.intellij.psi.impl.source.tree.injected.StringLiteralEscaper}
 */
public abstract class LiteralTextEscaperBase<T extends PsiLanguageInjectionHost> extends LiteralTextEscaper<T> {

    private int[] outSourceOffsets;

    public LiteralTextEscaperBase(@NotNull T host) {
        super(host);
    }

    @Override
    public boolean decode(@NotNull TextRange rangeInsideHost, @NotNull StringBuilder outChars) {
        String subText = rangeInsideHost.substring(myHost.getText());
        Pair<int[], Boolean> result = parseStringCharacters(subText, outChars);
        outSourceOffsets = result.getFirst();
        return result.getSecond();
    }

    @Override
    public int getOffsetInHost(int offsetInDecoded, @NotNull TextRange rangeInsideHost) {
        int[] offsets = outSourceOffsets;
        assert offsets != null;
        int result = (offsetInDecoded < offsets.length) ? offsets[offsetInDecoded] : -1;
        if (result == -1) {
            return -1;
        } else {
            return (result <= rangeInsideHost.getLength() ? result : rangeInsideHost.getLength()) + rangeInsideHost.getStartOffset();
        }
    }

    @NotNull
    protected abstract Pair<int[], Boolean> parseStringCharacters(@NotNull String chars, @NotNull StringBuilder outChars);
}
