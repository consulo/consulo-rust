/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.lang.core.resolve2;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Context for ModCollector.
 */
public class ModCollectorContext {
    @NotNull
    private final CrateDefMap defMap;
    @NotNull
    private final CollectorContext context;
    private final int macroDepth;
    @Nullable
    private final OnAddItem onAddItem;

    @FunctionalInterface
    public interface OnAddItem {
        boolean onAddItem(@NotNull ModData containingMod, @NotNull String name, @NotNull PerNs perNs, @NotNull Visibility visibility);
    }

    public ModCollectorContext(
        @NotNull CrateDefMap defMap,
        @NotNull CollectorContext context
    ) {
        this(defMap, context, 0, null);
    }

    public ModCollectorContext(
        @NotNull CrateDefMap defMap,
        @NotNull CollectorContext context,
        int macroDepth,
        @Nullable OnAddItem onAddItem
    ) {
        this.defMap = defMap;
        this.context = context;
        this.macroDepth = macroDepth;
        this.onAddItem = onAddItem;
    }

    @NotNull
    public CrateDefMap getDefMap() {
        return defMap;
    }

    @NotNull
    public CollectorContext getContext() {
        return context;
    }

    public int getMacroDepth() {
        return macroDepth;
    }

    public boolean isHangingMode() {
        return context.isHangingMode();
    }

    public boolean addItem(@NotNull ModData containingMod, @NotNull String name, @NotNull PerNs perNs, @NotNull Visibility visibility) {
        if (onAddItem != null) {
            return onAddItem.onAddItem(containingMod, name, perNs, visibility);
        }
        return containingMod.addVisibleItem(name, perNs);
    }
}
