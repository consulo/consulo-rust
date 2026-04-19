/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.lang.core.resolve2;

import kotlin.Triple;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.rust.lang.core.psi.RsProcMacroKind;
import org.rust.lang.core.resolve.Namespace;
import org.rust.lang.core.resolve2.util.DollarCrateMap;
import org.rust.stdext.HashCode;

import java.util.Set;

public class ProcMacroCallInfo implements MacroCallInfoBase {
    @NotNull
    private final ModData containingMod;
    @NotNull
    private final MacroIndex macroIndex;
    @NotNull
    private final AttrInfo[] attrs;
    @NotNull
    private final String body;
    @Nullable
    private final HashCode bodyHash;
    private final int depth;
    @NotNull
    private final DollarCrateMap dollarCrateMap;
    private final int endOfAttrsOffset;
    private final boolean fixupRustSyntaxErrors;
    @Nullable
    private final Triple<VisItem, Set<Namespace>, RsProcMacroKind> originalItem;

    public ProcMacroCallInfo(
        @NotNull ModData containingMod,
        @NotNull MacroIndex macroIndex,
        @NotNull AttrInfo[] attrs,
        @NotNull String body,
        @Nullable HashCode bodyHash,
        int depth,
        @NotNull DollarCrateMap dollarCrateMap,
        int endOfAttrsOffset,
        boolean fixupRustSyntaxErrors,
        @Nullable Triple<VisItem, Set<Namespace>, RsProcMacroKind> originalItem
    ) {
        this.containingMod = containingMod;
        this.macroIndex = macroIndex;
        this.attrs = attrs;
        this.body = body;
        this.bodyHash = bodyHash;
        this.depth = depth;
        this.dollarCrateMap = dollarCrateMap;
        this.endOfAttrsOffset = endOfAttrsOffset;
        this.fixupRustSyntaxErrors = fixupRustSyntaxErrors;
        this.originalItem = originalItem;
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
    public AttrInfo[] getAttrs() {
        return attrs;
    }

    @NotNull
    public String getBody() {
        return body;
    }

    @Nullable
    public HashCode getBodyHash() {
        return bodyHash;
    }

    public int getDepth() {
        return depth;
    }

    @NotNull
    public DollarCrateMap getDollarCrateMap() {
        return dollarCrateMap;
    }

    public int getEndOfAttrsOffset() {
        return endOfAttrsOffset;
    }

    public boolean isFixupRustSyntaxErrors() {
        return fixupRustSyntaxErrors;
    }

    @Nullable
    public Triple<VisItem, Set<Namespace>, RsProcMacroKind> getOriginalItem() {
        return originalItem;
    }

    public static class AttrInfo {
        @NotNull
        private final String[] path;
        private final int index;
        private final int deriveIndex;

        public AttrInfo(@NotNull String[] path, int index, int deriveIndex) {
            this.path = path;
            this.index = index;
            this.deriveIndex = deriveIndex;
        }

        @NotNull
        public String[] getPath() {
            return path;
        }

        /** -1 for derive macros */
        public int getIndex() {
            return index;
        }

        public int getDeriveIndex() {
            return deriveIndex;
        }
    }
}
