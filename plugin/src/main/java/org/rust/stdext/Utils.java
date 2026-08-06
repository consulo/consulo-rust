/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.stdext;

import consulo.document.util.TextRange;
import consulo.util.lang.StringUtil;
import consulo.virtualFileSystem.VirtualFile;
import org.apache.commons.lang3.RandomStringUtils;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

import java.util.function.Function;

public final class Utils {
    private Utils() {
    }

    @Nullable
    public static <T> T applyWithSymlink(@Nonnull VirtualFile file, @Nonnull Function<VirtualFile, T> f) {
        T result = f.apply(file);
        if (result != null) return result;
        VirtualFile canonical = file.getCanonicalFile();
        if (canonical == null) return null;
        return f.apply(canonical);
    }

    @Nonnull
    public static String pluralize(@Nonnull String str) {
        return StringUtil.pluralize(str);
    }

    @Nonnull
    public static String capitalized(@Nonnull String str) {
        return StringUtil.capitalize(str);
    }

    @Nonnull
    public static String randomLowercaseAlphabetic(int length) {
        return RandomStringUtils.random(length, "0123456789abcdefghijklmnopqrstuvwxyz");
    }

    @Nonnull
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
    @Nonnull
    public static TextRange stripWhitespace(@Nonnull TextRange range, @Nonnull CharSequence chars) {
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
