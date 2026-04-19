/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.lang.core.macros.proc;

import org.jetbrains.annotations.Nullable;

public enum ProMacroExpanderVersion {
    NO_VERSION_CHECK_VERSION,
    VERSION_CHECK_VERSION,
    ENCODE_CLOSE_SPAN_VERSION;

    @Nullable
    public static ProMacroExpanderVersion from(int i) {
        ProMacroExpanderVersion[] values = values();
        if (i >= 0 && i < values.length) {
            return values[i];
        }
        return null;
    }
}
