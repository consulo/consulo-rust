/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.lang.core.resolve2;

import jakarta.annotation.Nonnull;

/**
 * Lightweight info about a macro call, stored after expansion.
 */
public class MacroCallLightInfo {
    @Nonnull
    private final MacroIndex macroIndex;
    @Nonnull
    private final ModData containingMod;

    public MacroCallLightInfo(@Nonnull MacroIndex macroIndex, @Nonnull ModData containingMod) {
        this.macroIndex = macroIndex;
        this.containingMod = containingMod;
    }

    @Nonnull
    public MacroIndex getMacroIndex() {
        return macroIndex;
    }

    @Nonnull
    public ModData getContainingMod() {
        return containingMod;
    }
}
