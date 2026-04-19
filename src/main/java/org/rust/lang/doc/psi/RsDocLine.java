/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.lang.doc.psi;

import com.intellij.util.text.CharArrayUtil;
import com.intellij.util.text.CharSequenceSubSequence;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Objects;

public final class RsDocLine {
    private final CharSequence text;
    private final int startOffset;
    private final int endOffset;
    private final int contentStartOffset;
    private final int contentEndOffset;
    private final boolean lastLine;
    private final boolean removed;

    public RsDocLine(CharSequence text, int startOffset, int endOffset, int contentStartOffset, int contentEndOffset, boolean lastLine, boolean removed) {
        if (contentEndOffset < contentStartOffset) {
            throw new IllegalArgumentException("`" + text + "`, " + contentStartOffset + ", " + contentEndOffset);
        }
        this.text = text;
        this.startOffset = startOffset;
        this.endOffset = endOffset;
        this.contentStartOffset = contentStartOffset;
        this.contentEndOffset = contentEndOffset;
        this.lastLine = lastLine;
        this.removed = removed;
    }

    public RsDocLine(CharSequence text, int startOffset, int endOffset, boolean lastLine) {
        this(text, startOffset, endOffset, startOffset, endOffset, lastLine, false);
    }

    public CharSequence getPrefix() {
        return new CharSequenceSubSequence(text, startOffset, contentStartOffset);
    }

    public CharSequence getContent() {
        return new CharSequenceSubSequence(text, contentStartOffset, contentEndOffset);
    }

    public CharSequence getSuffix() {
        return new CharSequenceSubSequence(text, contentEndOffset, endOffset);
    }

    public boolean hasPrefix() { return startOffset != contentStartOffset; }
    public boolean hasContent() { return contentStartOffset != contentEndOffset; }
    public boolean hasSuffix() { return contentEndOffset != endOffset; }
    public boolean isLastLine() { return lastLine; }
    public boolean isRemoved() { return removed; }

    public int getContentStartOffset() { return contentStartOffset; }
    public int getContentLength() { return contentEndOffset - contentStartOffset; }

    public RsDocLine removePrefix(String prefix) {
        if (CharArrayUtil.regionMatches(text, contentStartOffset, contentEndOffset, prefix)) {
            return new RsDocLine(text, startOffset, endOffset, contentStartOffset + prefix.length(), contentEndOffset, lastLine, removed);
        }
        return this;
    }

    public RsDocLine removeSuffix(String suffix) {
        if (getContentLength() < suffix.length()) return this;
        if (CharArrayUtil.regionMatches(text, contentEndOffset - suffix.length(), contentEndOffset, suffix)) {
            return new RsDocLine(text, startOffset, endOffset, contentStartOffset, contentEndOffset - suffix.length(), lastLine, removed);
        }
        return this;
    }

    public RsDocLine markRemoved() {
        return new RsDocLine(text, startOffset, endOffset, contentStartOffset, contentStartOffset, lastLine, true);
    }

    public RsDocLine trimStart() {
        int newOffset = CharArrayUtil.shiftForward(text, contentStartOffset, contentEndOffset, " \t");
        return new RsDocLine(text, startOffset, endOffset, newOffset, contentEndOffset, lastLine, removed);
    }

    public int countStartWhitespace() {
        return CharArrayUtil.shiftForward(text, contentStartOffset, contentEndOffset, " \t") - contentStartOffset;
    }

    public RsDocLine substring(int startIndex) {
        if (startIndex > getContentLength()) throw new IllegalArgumentException();
        return new RsDocLine(text, startOffset, endOffset, contentStartOffset + startIndex, contentEndOffset, lastLine, removed);
    }

    @NotNull
    public static List<RsDocLine> splitLines(@NotNull CharSequence text) {
        List<RsDocLine> lines = new ArrayList<>();
        int prev = 0;
        while (true) {
            int index = indexOf(text, '\n', prev);
            if (index >= 0) {
                lines.add(new RsDocLine(text, prev, index, false));
                prev = index + 1;
            } else {
                lines.add(new RsDocLine(text, prev, text.length(), true));
                break;
            }
        }
        return lines;
    }

    private static int indexOf(CharSequence seq, char c, int startIndex) {
        for (int i = startIndex; i < seq.length(); i++) {
            if (seq.charAt(i) == c) return i;
        }
        return -1;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof RsDocLine)) return false;
        RsDocLine that = (RsDocLine) o;
        return startOffset == that.startOffset && endOffset == that.endOffset
            && contentStartOffset == that.contentStartOffset && contentEndOffset == that.contentEndOffset
            && lastLine == that.lastLine && removed == that.removed && Objects.equals(text, that.text);
    }

    @Override
    public int hashCode() {
        return Objects.hash(text, startOffset, endOffset, contentStartOffset, contentEndOffset, lastLine, removed);
    }
}
