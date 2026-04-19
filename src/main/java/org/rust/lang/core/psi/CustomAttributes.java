/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.lang.core.psi;

import com.intellij.psi.util.CachedValueProvider;
import com.intellij.psi.util.CachedValuesManager;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
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
    @NotNull Set<String> customAttrs,
    @NotNull Set<String> customTools
) {
    public static final CustomAttributes EMPTY = new CustomAttributes(Collections.emptySet(), Collections.emptySet());

    @NotNull
    public static CustomAttributes fromCrate(@NotNull Crate crate) {
        var project = crate.getProject();
        return CachedValuesManager.getManager(project).getCachedValue(crate, () ->
            CachedValueProvider.Result.create(doGetFromCrate(crate), RsPsiManagerUtil.getRustStructureModificationTracker(crate))
        );
    }

    @NotNull
    private static CustomAttributes doGetFromCrate(@NotNull Crate crate) {
        RsFile rootMod = crate.getRootMod();
        if (rootMod == null) return EMPTY;
        return fromRootModule(rootMod, crate);
    }

    @NotNull
    private static CustomAttributes fromRootModule(@NotNull RsFile rootMod, @NotNull Crate crate) {
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

    private static void collectMetaItemArgNames(@NotNull RsMetaItem meta, @NotNull Set<String> collector) {
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
