/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.cargo.runconfig.filters;

import org.jetbrains.annotations.NotNull;

public final class FilterUtils {

    public static final FilterUtils INSTANCE = new FilterUtils();

    private FilterUtils() {
    }

    /**
     * Normalizes function path:
     * - Removes angle brackets from the element path, including enclosed contents when necessary.
     * - Removes closure markers.
     * Examples:
     * - {@code <core::option::Option<T>>::unwrap -> core::option::Option::unwrap}
     * - {@code std::panicking::default_hook::{{closure}} -> std::panicking::default_hook}
     */
    @NotNull
    public static String normalizeFunctionPath(@NotNull String function) {
        String str = function;
        while (str.endsWith("::{{closure}}")) {
            int lastIdx = str.lastIndexOf("::");
            str = str.substring(0, lastIdx);
        }
        while (true) {
            int[] range = findAngleBrackets(str);
            if (range == null) break;
            int start = range[0];
            int end = range[1];
            int idx = str.indexOf("::", start + 1);
            if (idx < 0 || idx > end) {
                str = str.substring(0, start) + str.substring(end + 1);
            } else {
                // Remove the closing bracket first (higher index), then the opening bracket
                str = str.substring(0, end) + str.substring(end + 1);
                str = str.substring(0, start) + str.substring(start + 1);
            }
        }
        return str;
    }

    /**
     * Finds the range of the first matching angle brackets within the string.
     * Returns an int array [start, end] or null if not found.
     */
    private static int[] findAngleBrackets(@NotNull String str) {
        int start = -1;
        int counter = 0;
        for (int index = 0; index < str.length(); index++) {
            char ch = str.charAt(index);
            if (ch == '<') {
                if (start < 0) {
                    start = index;
                }
                counter += 1;
            } else if (ch == '>') {
                counter -= 1;
            } else {
                continue;
            }
            if (counter == 0) {
                return new int[]{start, index};
            }
        }
        return null;
    }
}
