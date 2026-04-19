/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.stdext;

import com.intellij.openapi.util.TextRange;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.openapi.vfs.VirtualFile;
import org.apache.commons.lang3.RandomStringUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.function.Function;

public final class Utils {
    private Utils() {
    }

    @Nullable
    public static <T> T applyWithSymlink(@NotNull VirtualFile file, @NotNull Function<VirtualFile, T> f) {
        T result = f.apply(file);
        if (result != null) return result;
        VirtualFile canonical = file.getCanonicalFile();
        if (canonical == null) return null;
        return f.apply(canonical);
    }

    @NotNull
    public static String pluralize(@NotNull String str) {
        return StringUtil.pluralize(str);
    }

    @NotNull
    public static String capitalized(@NotNull String str) {
        return StringUtil.capitalize(str);
    }

    @NotNull
    public static String randomLowercaseAlphabetic(int length) {
        return RandomStringUtils.random(length, "0123456789abcdefghijklmnopqrstuvwxyz");
    }

    @NotNull
    public static String numberSuffix(int number) {
        if ((number % 100) >= 11 && (number % 100) <= 13) {
            return "th";
        }
        switch (number % 10) {
            case 1: return "st";
            case 2: return "nd";
            case 3: return "rd";
            default: return "th";
        }
    }

    public static boolean isPowerOfTwo(long value) {
        return value > 0 && (value & (value - 1)) == 0L;
    }

    /**
     * Strips leading and trailing whitespace characters from the given range within the char sequence.
     */
    @NotNull
    public static TextRange stripWhitespace(@NotNull TextRange range, @NotNull CharSequence chars) {
        int start = range.getStartOffset();
        int end = range.getEndOffset();
        int length = chars.length();
        while (start < end && start < length && Character.isWhitespace(chars.charAt(start))) {
            start++;
        }
        while (end > start && end <= length && Character.isWhitespace(chars.charAt(end - 1))) {
            end--;
        }
        return TextRange.create(start, end);
    }
}
