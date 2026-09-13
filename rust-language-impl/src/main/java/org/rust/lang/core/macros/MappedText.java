/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.lang.core.macros;

import jakarta.annotation.Nonnull;

import java.util.Objects;

public final class MappedText {
    public static final MappedText EMPTY = new MappedText("", RangeMap.EMPTY);

    private final String myText;
    private final RangeMap myRanges;

    public MappedText(@Nonnull String text, @Nonnull RangeMap ranges) {
        myText = text;
        myRanges = ranges;
    }

    @Nonnull
    public String getText() {
        return myText;
    }

    @Nonnull
    public RangeMap getRanges() {
        return myRanges;
    }

    @Nonnull
    public static MappedText single(@Nonnull String text, int srcOffset) {
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
