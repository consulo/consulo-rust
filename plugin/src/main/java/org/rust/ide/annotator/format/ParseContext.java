/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.ide.annotator.format;

import consulo.document.util.TextRange;
import jakarta.annotation.Nonnull;

import java.util.List;

public class ParseContext {
    @Nonnull
    private final int[] mySourceMap;
    private final int myOffset;
    @Nonnull
    private final List<ParsedParameter> myParameters;

    public ParseContext(@Nonnull int[] sourceMap, int offset, @Nonnull List<ParsedParameter> parameters) {
        this.mySourceMap = sourceMap;
        this.myOffset = offset;
        this.myParameters = parameters;
    }

    @Nonnull
    public int[] getSourceMap() {
        return mySourceMap;
    }

    public int getOffset() {
        return myOffset;
    }

    @Nonnull
    public List<ParsedParameter> getParameters() {
        return myParameters;
    }

    @Nonnull
    public TextRange toSourceRange(int rangeStart, int rangeEnd, int additionalOffset) {
        return new TextRange(
            mySourceMap[rangeStart + additionalOffset],
            mySourceMap[rangeEnd + additionalOffset] + 1
        ).shiftRight(myOffset);
    }

    @Nonnull
    public TextRange toSourceRange(int rangeStart, int rangeEnd) {
        return toSourceRange(rangeStart, rangeEnd, 0);
    }
}
