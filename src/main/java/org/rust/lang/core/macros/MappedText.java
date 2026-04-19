/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.lang.core.macros;

import org.jetbrains.annotations.NotNull;

import java.util.Objects;

public final class MappedText {
    public static final MappedText EMPTY = new MappedText("", RangeMap.EMPTY);

    private final String myText;
    private final RangeMap myRanges;

    public MappedText(@NotNull String text, @NotNull RangeMap ranges) {
        myText = text;
        myRanges = ranges;
    }

    @NotNull
    public String getText() {
        return myText;
    }

    @NotNull
    public RangeMap getRanges() {
        return myRanges;
    }

    @NotNull
    public static MappedText single(@NotNull String text, int srcOffset) {
        if (!text.isEmpty()) {
            return new MappedText(
                text,
                new RangeMap(new MappedTextRange(srcOffset, 0, text.length()))
            );
        } else {
            return EMPTY;
        }
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        MappedText that = (MappedText) o;
        return myText.equals(that.myText) && myRanges.equals(that.myRanges);
    }

    @Override
    public int hashCode() {
        return Objects.hash(myText, myRanges);
    }

    @Override
    public String toString() {
        return "MappedText(text=" + myText + ", ranges=" + myRanges + ")";
    }
}
