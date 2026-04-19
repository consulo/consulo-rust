/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.lang.core.macros;

import com.intellij.openapi.util.TextRange;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Objects;

public final class MappedTextRange {
    private final int mySrcOffset;
    private final int myDstOffset;
    private final int myLength;

    public MappedTextRange(int srcOffset, int dstOffset, int length) {
        if (srcOffset < 0) {
            throw new IllegalArgumentException("srcOffset should be >= 0; got: " + srcOffset);
        }
        if (dstOffset < 0) {
            throw new IllegalArgumentException("dstOffset should be >= 0; got: " + dstOffset);
        }
        if (length <= 0) {
            throw new IllegalArgumentException("length should be greater than 0; got: " + length);
        }
        mySrcOffset = srcOffset;
        myDstOffset = dstOffset;
        myLength = length;
    }

    public int getSrcOffset() {
        return mySrcOffset;
    }

    public int getDstOffset() {
        return myDstOffset;
    }

    public int getLength() {
        return myLength;
    }

    public int getSrcEndOffset() {
        return mySrcOffset + myLength;
    }

    public int getDstEndOffset() {
        return myDstOffset + myLength;
    }

    @NotNull
    public TextRange getSrcRange() {
        return new TextRange(mySrcOffset, mySrcOffset + myLength);
    }

    @NotNull
    public TextRange getDstRange() {
        return new TextRange(myDstOffset, myDstOffset + myLength);
    }

    @NotNull
    public MappedTextRange srcShiftLeft(int delta) {
        return new MappedTextRange(mySrcOffset - delta, myDstOffset, myLength);
    }

    @NotNull
    public MappedTextRange dstShiftRight(int delta) {
        return new MappedTextRange(mySrcOffset, myDstOffset + delta, myLength);
    }

    @NotNull
    public MappedTextRange shiftRight(int delta) {
        return new MappedTextRange(mySrcOffset + delta, myDstOffset + delta, myLength);
    }

    @NotNull
    public MappedTextRange withLength(int length) {
        return new MappedTextRange(mySrcOffset, myDstOffset, length);
    }

    @Nullable
    MappedTextRange dstIntersection(@NotNull TextRange range) {
        int newDstStart = Math.max(myDstOffset, range.getStartOffset());
        int newDstEnd = Math.min(getDstEndOffset(), range.getEndOffset());
        if (newDstStart < newDstEnd) {
            int srcDelta = newDstStart - myDstOffset;
            return new MappedTextRange(
                mySrcOffset + srcDelta,
                newDstStart,
                newDstEnd - newDstStart
            );
        } else {
            return null;
        }
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        MappedTextRange that = (MappedTextRange) o;
        return mySrcOffset == that.mySrcOffset && myDstOffset == that.myDstOffset && myLength == that.myLength;
    }

    @Override
    public int hashCode() {
        return Objects.hash(mySrcOffset, myDstOffset, myLength);
    }

    @Override
    public String toString() {
        return "MappedTextRange(srcOffset=" + mySrcOffset + ", dstOffset=" + myDstOffset + ", length=" + myLength + ")";
    }
}
