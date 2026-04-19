/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.ide.hints.parameter;

import com.intellij.openapi.util.TextRange;
import org.jetbrains.annotations.NotNull;

import java.util.List;

/**
 * Stores the text representation and ranges for parameters
 */
public class HintLine {
    private final String myPresentText;
    private final List<TextRange> myRanges;

    public HintLine(@NotNull String presentText, @NotNull List<TextRange> ranges) {
        this.myPresentText = presentText;
        this.myRanges = ranges;
    }

    @NotNull
    public String getPresentText() {
        return myPresentText;
    }

    @NotNull
    public TextRange getRange(int index) {
        if (index < 0 || index >= myRanges.size()) return TextRange.EMPTY_RANGE;
        return myRanges.get(index);
    }
}
