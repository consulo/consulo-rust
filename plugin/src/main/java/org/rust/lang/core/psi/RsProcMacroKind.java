/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.lang.core.psi;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import org.rust.lang.core.RsPsiPattern;
import org.rust.lang.core.psi.ext.RsMetaItemUtil;
import org.rust.lang.core.psi.ext.QueryAttributes;

public enum RsProcMacroKind {
    /**
     * Function-like proc macro: {@code foo!();}
     */
    FUNCTION_LIKE,

    /**
     * Derive proc macro: {@code #[derive(Foo)]}
     */
    DERIVE,

    /**
     * Attribute proc macro: {@code #[foo]}
     */
    ATTRIBUTE;

    @Nullable
    public static RsProcMacroKind fromDefAttributes(@Nonnull org.rust.lang.core.psi.ext.QueryAttributes<?> attrs) {
        for (var meta : attrs.getMetaItems()) {
            String name = RsMetaItemUtil.getName(meta);
            if ("proc_macro".equals(name)) return FUNCTION_LIKE;
            if ("proc_macro_attribute".equals(name)) return ATTRIBUTE;
            if ("proc_macro_derive".equals(name)) return DERIVE;
        }
        return null;
    }

    /** Internal. Don't use it. It can return non-null value for an item that is not a proc macro call */
    @Nullable
    public static RsProcMacroKind fromMacroCall(@Nonnull RsMetaItem metaItem) {
        if (RsPsiPattern.derivedTraitMetaItem.accepts(metaItem)) return DERIVE;
        if (RsMetaItemUtil.isRootMetaItem(metaItem)) return ATTRIBUTE;
        return null;
    }
}
