/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.cargo.api.settings;

import com.intellij.openapi.options.advanced.AdvancedSettings;

public final class RustAdvancedSettings {

    public static final String MACROS_MAXIMUM_RECURSION_LIMIT_SETTING_KEY = "org.rust.macros.maximum.recursion.limit";

    private static final int MACROS_MAXIMUM_RECURSION_LIMIT_DEFAULT = 1000;

    private RustAdvancedSettings() {
    }

    public static int getMaximumRecursionLimit() {
        int value = AdvancedSettings.getInt(MACROS_MAXIMUM_RECURSION_LIMIT_SETTING_KEY,
            MACROS_MAXIMUM_RECURSION_LIMIT_DEFAULT);
        return value <= 0 ? Integer.MAX_VALUE : value;
    }
}
