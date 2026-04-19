/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.lang.core.psi;

import org.jetbrains.annotations.Nullable;
import org.rust.lang.core.crate.Crate;
import org.rust.lang.core.psi.ext.RsAttrProcMacroOwner;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

/**
 * A simple single-thread cache used for caching of attribute macros
 * during per-file operations like highlighting
 */
public abstract sealed class AttrCache permits AttrCache.NoCache, AttrCache.HashMapCache {

    @Nullable
    public abstract RsMetaItem cachedGetProcMacroAttribute(RsAttrProcMacroOwner owner);

    public static final class NoCache extends AttrCache {
        public static final NoCache INSTANCE = new NoCache();

        private NoCache() {}

        @Override
        @Nullable
        public RsMetaItem cachedGetProcMacroAttribute(RsAttrProcMacroOwner owner) {
            ProcMacroAttribute<?> procMacroAttribute = owner.getProcMacroAttribute();
            if (procMacroAttribute == null) return null;
            Object attr = procMacroAttribute.getAttr();
            return attr instanceof RsMetaItem ? (RsMetaItem) attr : null;
        }
    }

    public static final class HashMapCache extends AttrCache {
        private final @Nullable Crate crate;
        private final Map<RsAttrProcMacroOwner, Optional<RsMetaItem>> cache = new HashMap<>();

        public HashMapCache(@Nullable Crate crate) {
            this.crate = crate;
        }

        @Override
        @Nullable
        public RsMetaItem cachedGetProcMacroAttribute(RsAttrProcMacroOwner owner) {
            return cache.computeIfAbsent(owner, o -> {
                ProcMacroAttribute<RsMetaItem> attr = ProcMacroAttribute.getProcMacroAttribute(o, null, crate, false, false);
                return Optional.ofNullable(attr != null ? attr.getAttr() : null);
            }).orElse(null);
        }
    }
}
