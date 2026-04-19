/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.lang.core.resolve2;

import org.jetbrains.annotations.NotNull;

/**
 * Lightweight info about a macro call, stored after expansion.
 */
public class MacroCallLightInfo {
    @NotNull
    private final MacroIndex macroIndex;
    @NotNull
    private final ModData containingMod;

    public MacroCallLightInfo(@NotNull MacroIndex macroIndex, @NotNull ModData containingMod) {
        this.macroIndex = macroIndex;
        this.containingMod = containingMod;
    }

    @NotNull
    public MacroIndex getMacroIndex() {
        return macroIndex;
    }

    @NotNull
    public ModData getContainingMod() {
        return containingMod;
    }
}
