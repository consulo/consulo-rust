/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.lang.core.resolve2;
import org.rust.lang.core.resolve.SelectionCandidate.Triple;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import org.rust.lang.core.psi.RsProcMacroKind;
import org.rust.lang.core.resolve.Namespace;
import org.rust.lang.core.resolve2.util.DollarCrateMap;
import org.rust.stdext.HashCode;

import java.util.Set;
import org.rust.lang.core.resolve.SelectionCandidate;

public class ProcMacroCallInfo implements MacroCallInfoBase {
    @Nonnull
    private final ModData containingMod;
    @Nonnull
    private final MacroIndex macroIndex;
    @Nonnull
    private final AttrInfo[] attrs;
    @Nonnull
    private final String body;
    @Nullable
    private final HashCode bodyHash;
    private final int depth;
    @Nonnull
    private final DollarCrateMap dollarCrateMap;
    private final int endOfAttrsOffset;
    private final boolean fixupRustSyntaxErrors;
    @Nullable
    private final Triple<VisItem, Set<Namespace>, RsProcMacroKind> originalItem;

    public ProcMacroCallInfo(
        @Nonnull ModData containingMod,
        @Nonnull MacroIndex macroIndex,
        @Nonnull AttrInfo[] attrs,
        @Nonnull String body,
        @Nullable HashCode bodyHash,
        int depth,
        @Nonnull DollarCrateMap dollarCrateMap,
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
    @Nonnull
    public ModData getContainingMod() {
        return containingMod;
    }

    @Nonnull
    public MacroIndex getMacroIndex() {
        return macroIndex;
    }

    @Nonnull
    public AttrInfo[] getAttrs() {
        return attrs;
    }

    @Nonnull
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

    @Nonnull
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
        @Nonnull
        private final String[] path;
        private final int index;
        private final int deriveIndex;

        public AttrInfo(@Nonnull String[] path, int index, int deriveIndex) {
            this.path = path;
            this.index = index;
            this.deriveIndex = deriveIndex;
        }

        @Nonnull
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
