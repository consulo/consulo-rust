/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.lang.core.resolve2;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.rust.lang.core.macros.MacroCallBody;
import org.rust.lang.core.resolve2.util.DollarCrateMap;
import org.rust.stdext.HashCode;

public class MacroCallInfo implements MacroCallInfoBase {
    @NotNull
    private final ModData containingMod;
    @NotNull
    private final MacroIndex macroIndex;
    @NotNull
    private final String[] path;
    @NotNull
    private final MacroCallBody body;
    @Nullable
    private final HashCode bodyHash;
    @Nullable
    private final Integer containingFileId;
    private final int depth;
    @NotNull
    private final DollarCrateMap dollarCrateMap;

    public MacroCallInfo(
        @NotNull ModData containingMod,
        @NotNull MacroIndex macroIndex,
        @NotNull String[] path,
        @NotNull MacroCallBody body,
        @Nullable HashCode bodyHash,
        @Nullable Integer containingFileId,
        int depth,
        @NotNull DollarCrateMap dollarCrateMap
    ) {
        this.containingMod = containingMod;
        this.macroIndex = macroIndex;
        this.path = path;
        this.body = body;
        this.bodyHash = bodyHash;
        this.containingFileId = containingFileId;
        this.depth = depth;
        this.dollarCrateMap = dollarCrateMap;
    }

    @Override
    @NotNull
    public ModData getContainingMod() {
        return containingMod;
    }

    @NotNull
    public MacroIndex getMacroIndex() {
        return macroIndex;
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
    public HashCode getBodyHash() {
        return bodyHash;
    }

    @Nullable
    public Integer getContainingFileId() {
        return containingFileId;
    }

    public int getDepth() {
        return depth;
    }

    @NotNull
    public DollarCrateMap getDollarCrateMap() {
        return dollarCrateMap;
    }

    @Override
    @NotNull
    public String toString() {
        return containingMod.getPath() + ":  " + String.join("::", path) + "! { " + body + " }";
    }
}
