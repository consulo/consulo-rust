/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.ide.hints.parameter;

import consulo.document.util.TextRange;
import jakarta.annotation.Nonnull;

import java.util.List;

/**
 * Stores the text representation and ranges for parameters
 */
public class HintLine {
    private final String myPresentText;
    private final List<TextRange> myRanges;

    public HintLine(@Nonnull String presentText, @Nonnull List<TextRange> ranges) {
        this.myPresentText = presentText;
        this.myRanges = ranges;
    }

    @Nonnull
    public String getPresentText() {
        return myPresentText;
    }

    @Nonnull
    public TextRange getRange(int index) {
        if (index < 0 || index >= myRanges.size()) return TextRange.EMPTY_RANGE;
        return myRanges.get(index);
    }
}
