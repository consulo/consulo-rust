/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.lang.core.psi;

import com.intellij.openapi.util.TextRange;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Stores offsets of distinguishable parts of a literal.
 */
public record LiteralOffsets(
    @Nullable TextRange prefix,
    @Nullable TextRange openDelim,
    @Nullable TextRange value,
    @Nullable TextRange closeDelim,
    @Nullable TextRange suffix
) {
    public LiteralOffsets() {
        this(null, null, null, null, null);
    }

    @Nullable public TextRange getPrefix() { return prefix; }
    @Nullable public TextRange getOpenDelim() { return openDelim; }
    @Nullable public TextRange getValue() { return value; }
    @Nullable public TextRange getCloseDelim() { return closeDelim; }
    @Nullable public TextRange getSuffix() { return suffix; }

    @NotNull
    public static LiteralOffsets fromEndOffsets(int prefixEnd, int openDelimEnd, int valueEnd,
                                                 int closeDelimEnd, int suffixEnd) {
        TextRange prefix = makeRange(0, prefixEnd);
        TextRange openDelim = makeRange(prefixEnd, openDelimEnd);

        TextRange value = makeRange(openDelimEnd, valueEnd);
        // empty value is still a value provided we have open delimiter
        if (value == null && openDelim != null) {
            value = TextRange.create(openDelimEnd, openDelimEnd);
        }

        TextRange closeDelim = makeRange(valueEnd, closeDelimEnd);
        TextRange suffix = makeRange(closeDelimEnd, suffixEnd);

        return new LiteralOffsets(prefix, openDelim, value, closeDelim, suffix);
    }

    @Nullable
    private static TextRange makeRange(int start, int end) {
        if (end - start > 0) {
            return new TextRange(start, end);
        }
        return null;
    }
}
