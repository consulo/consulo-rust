/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.lang.core.macros;

import jakarta.annotation.Nonnull;
import org.rust.lang.core.macros.builtin.BuiltinMacroExpander;
import org.rust.stdext.HashCode;

public class RsBuiltinMacroData extends RsMacroData {
    private static final HashCode BUILTIN_DEF_HASH = HashCode.compute(String.valueOf(BuiltinMacroExpander.EXPANDER_VERSION));

    private final String myName;

    public RsBuiltinMacroData(@Nonnull String name) {
        myName = name;
    }

    @Nonnull
    public String getName() {
        return myName;
    }

    @Nonnull
    public RsMacroDataWithHash<RsBuiltinMacroData> withHash() {
        return new RsMacroDataWithHash<>(this, HashCode.mix(HashCode.compute(myName), BUILTIN_DEF_HASH));
    }
}
