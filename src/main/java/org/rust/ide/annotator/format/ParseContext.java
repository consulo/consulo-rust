/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.ide.annotator.format;

import com.intellij.openapi.util.TextRange;
import org.jetbrains.annotations.NotNull;

import java.util.List;

public class ParseContext {
    @NotNull
    private final int[] mySourceMap;
    private final int myOffset;
    @NotNull
    private final List<ParsedParameter> myParameters;

    public ParseContext(@NotNull int[] sourceMap, int offset, @NotNull List<ParsedParameter> parameters) {
        this.mySourceMap = sourceMap;
        this.myOffset = offset;
        this.myParameters = parameters;
    }

    @NotNull
    public int[] getSourceMap() {
        return mySourceMap;
    }

    public int getOffset() {
        return myOffset;
    }

    @NotNull
    public List<ParsedParameter> getParameters() {
        return myParameters;
    }

    @NotNull
    public TextRange toSourceRange(int rangeStart, int rangeEnd, int additionalOffset) {
        return new TextRange(
            mySourceMap[rangeStart + additionalOffset],
            mySourceMap[rangeEnd + additionalOffset] + 1
        ).shiftRight(myOffset);
    }

    @NotNull
    public TextRange toSourceRange(int rangeStart, int rangeEnd) {
        return toSourceRange(rangeStart, rangeEnd, 0);
    }
}
