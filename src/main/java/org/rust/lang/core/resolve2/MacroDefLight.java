/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.lang.core.resolve2;

import org.jetbrains.annotations.NotNull;

/**
 * Lightweight representation of macro definition items.
 */
public class MacroDefLight {
    @NotNull
    private final String name;
    @NotNull
    private final String body;
    @NotNull
    private final String bodyHash;
    private final boolean hasMacroExport;
    private final boolean hasLocalInnerMacros;
    private final boolean hasRustcBuiltinMacro;
    private final boolean isDeeplyEnabledByCfg;
    private final int macroIndexInParent;

    public MacroDefLight(
        @NotNull String name,
        @NotNull String body,
        @NotNull String bodyHash,
        boolean hasMacroExport,
        boolean hasLocalInnerMacros,
        boolean hasRustcBuiltinMacro,
        boolean isDeeplyEnabledByCfg
    ) {
        this(name, body, bodyHash, hasMacroExport, hasLocalInnerMacros, hasRustcBuiltinMacro, isDeeplyEnabledByCfg, 0);
    }

    public MacroDefLight(
        @NotNull String name,
        @NotNull String body,
        @NotNull String bodyHash,
        boolean hasMacroExport,
        boolean hasLocalInnerMacros,
        boolean hasRustcBuiltinMacro,
        boolean isDeeplyEnabledByCfg,
        int macroIndexInParent
    ) {
        this.name = name;
        this.body = body;
        this.bodyHash = bodyHash;
        this.hasMacroExport = hasMacroExport;
        this.hasLocalInnerMacros = hasLocalInnerMacros;
        this.hasRustcBuiltinMacro = hasRustcBuiltinMacro;
        this.isDeeplyEnabledByCfg = isDeeplyEnabledByCfg;
        this.macroIndexInParent = macroIndexInParent;
    }

    @NotNull
    public String getName() {
        return name;
    }

    @NotNull
    public String getBody() {
        return body;
    }

    @NotNull
    public String getBodyHash() {
        return bodyHash;
    }

    public boolean isHasMacroExport() {
        return hasMacroExport;
    }

    public boolean isHasLocalInnerMacros() {
        return hasLocalInnerMacros;
    }

    public boolean isHasRustcBuiltinMacro() {
        return hasRustcBuiltinMacro;
    }

    public boolean isDeeplyEnabledByCfg() {
        return isDeeplyEnabledByCfg;
    }

    public int getMacroIndexInParent() {
        return macroIndexInParent;
    }
}
