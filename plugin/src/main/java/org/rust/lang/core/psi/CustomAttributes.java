/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.lang.core.psi;

import consulo.application.util.CachedValueProvider;
import consulo.application.util.CachedValuesManager;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import org.rust.lang.core.crate.Crate;
import org.rust.lang.core.psi.ext.RsMetaItemUtil;
import org.rust.lang.core.psi.ext.RsDocAndAttributeOwnerUtil;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

/**
 * {@code #![register_attr()]} and {@code #![register_tool()]} crate-root attributes
 */
public record CustomAttributes(
    @Nonnull Set<String> customAttrs,
    @Nonnull Set<String> customTools
) {
    public static final CustomAttributes EMPTY = new CustomAttributes(Collections.emptySet(), Collections.emptySet());

    @Nonnull
    public static CustomAttributes fromCrate(@Nonnull Crate crate) {
        var project = crate.getProject();
        return CachedValuesManager.getManager(project).getCachedValue(crate, () ->
            CachedValueProvider.Result.create(doGetFromCrate(crate), RsPsiManagerUtil.getRustStructureModificationTracker(crate))
        );
    }

    @Nonnull
    private static CustomAttributes doGetFromCrate(@Nonnull Crate crate) {
        RsFile rootMod = crate.getRootMod();
        if (rootMod == null) return EMPTY;
        return fromRootModule(rootMod, crate);
    }

    @Nonnull
    private static CustomAttributes fromRootModule(@Nonnull RsFile rootMod, @Nonnull Crate crate) {
        Set<String> attrs = new HashSet<>();
        Set<String> tools = new HashSet<>();
        var queryAttributes = RsDocAndAttributeOwnerUtil.getQueryAttributes(rootMod, crate);
        for (var meta : queryAttributes.getMetaItems()) {
            String name = RsMetaItemUtil.getName(meta);
            if ("register_attr".equals(name)) {
                collectMetaItemArgNames(meta, attrs);
            } else if ("register_tool".equals(name)) {
                collectMetaItemArgNames(meta, tools);
            }
        }
        return new CustomAttributes(attrs, tools);
    }

    private static void collectMetaItemArgNames(@Nonnull RsMetaItem meta, @Nonnull Set<String> collector) {
        var args = meta.getMetaItemArgs();
        if (args == null) return;
        for (var attr : args.getMetaItemList()) {
            String name = RsMetaItemUtil.getName(attr);
            if (name != null) {
                collector.add(name);
            }
        }
    }
}
