/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.lang.core.macros;

import com.intellij.util.SmartList;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;

public class MutableMappedText {
    private final StringBuilder mySb;
    private final List<MappedTextRange> myRanges;

    public MutableMappedText(int capacity) {
        mySb = new StringBuilder(capacity);
        myRanges = new ArrayList<>();
    }

    public int getLength() {
        return mySb.length();
    }

    @NotNull
    public CharSequence getText() {
        return mySb;
    }

    public void appendUnmapped(@NotNull CharSequence text) {
        mySb.append(text);
    }

    public void appendMapped(@NotNull CharSequence text, int srcOffset) {
        if (text.length() > 0) {
            RangeMap.mergeAdd(myRanges, new MappedTextRange(srcOffset, mySb.length(), text.length()));
            mySb.append(text);
        }
    }

    @NotNull
    public MappedText toMappedText() {
        return new MappedText(mySb.toString(), new RangeMap(new SmartList<>(myRanges)));
    }

    @Override
    public String toString() {
        return mySb.toString();
    }
}
