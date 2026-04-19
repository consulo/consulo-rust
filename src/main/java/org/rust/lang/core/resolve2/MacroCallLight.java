/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.lang.core.resolve2;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.rust.lang.core.macros.MacroCallBody;

/**
 * Lightweight representation of macro call items for hash calculation.
 */
public class MacroCallLight {
    @NotNull
    private final String[] path;
    @NotNull
    private final MacroCallBody body;
    @Nullable
    private final org.rust.stdext.HashCode bodyHash;
    private final boolean isDeeplyEnabledByCfg;
    private final int macroIndexInParent;
    private final int pathOffsetInExpansion;
    private final int bodyStartOffsetInExpansion;
    private final int bodyEndOffsetInExpansion;

    public MacroCallLight(
        @NotNull String[] path,
        @NotNull MacroCallBody body,
        @Nullable org.rust.stdext.HashCode bodyHash,
        boolean isDeeplyEnabledByCfg
    ) {
        this(path, body, bodyHash, isDeeplyEnabledByCfg, 0, 0, 0, 0);
    }

    public MacroCallLight(
        @NotNull String[] path,
        @NotNull MacroCallBody body,
        @Nullable org.rust.stdext.HashCode bodyHash,
        boolean isDeeplyEnabledByCfg,
        int macroIndexInParent,
        int pathOffsetInExpansion,
        int bodyStartOffsetInExpansion,
        int bodyEndOffsetInExpansion
    ) {
        this.path = path;
        this.body = body;
        this.bodyHash = bodyHash;
        this.isDeeplyEnabledByCfg = isDeeplyEnabledByCfg;
        this.macroIndexInParent = macroIndexInParent;
        this.pathOffsetInExpansion = pathOffsetInExpansion;
        this.bodyStartOffsetInExpansion = bodyStartOffsetInExpansion;
        this.bodyEndOffsetInExpansion = bodyEndOffsetInExpansion;
    }

    @NotNull
    public String[] getPath() {
        return path;
    }

    @NotNull
    public MacroCallBody getBody() {
        return body;
    }

    @Nullable
    public org.rust.stdext.HashCode getBodyHash() {
        return bodyHash;
    }

    public boolean isDeeplyEnabledByCfg() {
        return isDeeplyEnabledByCfg;
    }

    public int getMacroIndexInParent() {
        return macroIndexInParent;
    }

    public int getPathOffsetInExpansion() {
        return pathOffsetInExpansion;
    }

    public int getBodyStartOffsetInExpansion() {
        return bodyStartOffsetInExpansion;
    }

    public int getBodyEndOffsetInExpansion() {
        return bodyEndOffsetInExpansion;
    }
}
