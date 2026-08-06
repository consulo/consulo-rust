/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.lang.core.resolve2;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

/**
 * Context for ModCollector.
 */
public class ModCollectorContext {
    @Nonnull
    private final CrateDefMap defMap;
    @Nonnull
    private final CollectorContext context;
    private final int macroDepth;
    @Nullable
    private final OnAddItem onAddItem;

    @FunctionalInterface
    public interface OnAddItem {
        boolean onAddItem(@Nonnull ModData containingMod, @Nonnull String name, @Nonnull PerNs perNs, @Nonnull Visibility visibility);
    }

    public ModCollectorContext(
        @Nonnull CrateDefMap defMap,
        @Nonnull CollectorContext context
    ) {
        this(defMap, context, 0, null);
    }

    public ModCollectorContext(
        @Nonnull CrateDefMap defMap,
        @Nonnull CollectorContext context,
        int macroDepth,
        @Nullable OnAddItem onAddItem
    ) {
        this.defMap = defMap;
        this.context = context;
        this.macroDepth = macroDepth;
        this.onAddItem = onAddItem;
    }

    @Nonnull
    public CrateDefMap getDefMap() {
        return defMap;
    }

    @Nonnull
    public CollectorContext getContext() {
        return context;
    }

    public int getMacroDepth() {
        return macroDepth;
    }

    public boolean isHangingMode() {
        return context.isHangingMode();
    }

    public boolean addItem(@Nonnull ModData containingMod, @Nonnull String name, @Nonnull PerNs perNs, @Nonnull Visibility visibility) {
        if (onAddItem != null) {
            return onAddItem.onAddItem(containingMod, name, perNs, visibility);
        }
        return containingMod.addVisibleItem(name, perNs);
    }
}
