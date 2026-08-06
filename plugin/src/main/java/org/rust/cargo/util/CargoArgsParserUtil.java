/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.cargo.util;

import java.util.Collections;
import java.util.List;

public final class CargoArgsParserUtil {

    private CargoArgsParserUtil() {
    }

    /**
     * Splits a list of arguments on "--", returning a list of two lists:
     * [0] = arguments before "--", [1] = arguments after "--".
     */
    public static List<List<String>> splitOnDoubleDash(List<String> arguments) {
        int idx = arguments.indexOf("--");
        if (idx == -1) {
            return List.of(arguments, Collections.emptyList());
        }
        return List.of(arguments.subList(0, idx), arguments.subList(idx + 1, arguments.size()));
    }
}
