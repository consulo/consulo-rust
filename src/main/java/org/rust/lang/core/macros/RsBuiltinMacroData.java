/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.lang.core.macros;

import org.jetbrains.annotations.NotNull;
import org.rust.lang.core.macros.builtin.BuiltinMacroExpander;
import org.rust.stdext.HashCode;

public class RsBuiltinMacroData extends RsMacroData {
    private static final HashCode BUILTIN_DEF_HASH = HashCode.compute(String.valueOf(BuiltinMacroExpander.EXPANDER_VERSION));

    private final String myName;

    public RsBuiltinMacroData(@NotNull String name) {
        myName = name;
    }

    @NotNull
    public String getName() {
        return myName;
    }

    @NotNull
    public RsMacroDataWithHash<RsBuiltinMacroData> withHash() {
        return new RsMacroDataWithHash<>(this, HashCode.mix(HashCode.compute(myName), BUILTIN_DEF_HASH));
    }
}
