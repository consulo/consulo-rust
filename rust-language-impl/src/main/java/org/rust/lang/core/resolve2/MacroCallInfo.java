/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.lang.core.resolve2;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import org.rust.lang.core.macros.MacroCallBody;
import org.rust.lang.core.resolve2.util.DollarCrateMap;
import org.rust.stdext.HashCode;

public class MacroCallInfo implements MacroCallInfoBase {
    @Nonnull
    private final ModData containingMod;
    @Nonnull
    private final MacroIndex macroIndex;
    @Nonnull
    private final String[] path;
    @Nonnull
    private final MacroCallBody body;
    @Nullable
    private final HashCode bodyHash;
    @Nullable
    private final Integer containingFileId;
    private final int depth;
    @Nonnull
    private final DollarCrateMap dollarCrateMap;

    public MacroCallInfo(
        @Nonnull ModData containingMod,
        @Nonnull MacroIndex macroIndex,
        @Nonnull String[] path,
        @Nonnull MacroCallBody body,
        @Nullable HashCode bodyHash,
        @Nullable Integer containingFileId,
        int depth,
        @Nonnull DollarCrateMap dollarCrateMap
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
    @Nonnull
    public ModData getContainingMod() {
        return containingMod;
    }

    @Nonnull
    public MacroIndex getMacroIndex() {
        return macroIndex;
    }

    @Nonnull
    public String[] getPath() {
        return path;
    }

    @Nonnull
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

    @Nonnull
    public DollarCrateMap getDollarCrateMap() {
        return dollarCrateMap;
    }

    @Override
    @Nonnull
    public String toString() {
        return containingMod.getPath() + ":  " + String.join("::", path) + "! { " + body + " }";
    }
}
