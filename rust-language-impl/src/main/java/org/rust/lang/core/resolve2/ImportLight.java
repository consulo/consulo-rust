/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.lang.core.resolve2;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

/**
 * Lightweight representation of import items.
 */
public class ImportLight {
    @Nonnull
    private final String[] usePath;
    @Nullable
    private final String alias;
    @Nonnull
    private final VisibilityLight visibility;
    private final boolean isGlob;
    private final boolean isExternCrate;
    private final boolean isPrelude;
    private final boolean isDeeplyEnabledByCfg;
    private final boolean isMacroUse;
    private final int offsetInExpansion;

    public ImportLight(
        @Nonnull String[] usePath,
        @Nullable String alias,
        @Nonnull VisibilityLight visibility,
        boolean isGlob,
        boolean isExternCrate,
        boolean isPrelude,
        boolean isDeeplyEnabledByCfg,
        int offsetInExpansion
    ) {
        this(usePath, alias, visibility, isGlob, isExternCrate, isPrelude, isDeeplyEnabledByCfg, false, offsetInExpansion);
    }

    public ImportLight(
        @Nonnull String[] usePath,
        @Nullable String alias,
        @Nonnull VisibilityLight visibility,
        boolean isGlob,
        boolean isExternCrate,
        boolean isPrelude,
        boolean isDeeplyEnabledByCfg,
        boolean isMacroUse,
        int offsetInExpansion
    ) {
        this.usePath = usePath;
        this.alias = alias;
        this.visibility = visibility;
        this.isGlob = isGlob;
        this.isExternCrate = isExternCrate;
        this.isPrelude = isPrelude;
        this.isDeeplyEnabledByCfg = isDeeplyEnabledByCfg;
        this.isMacroUse = isMacroUse;
        this.offsetInExpansion = offsetInExpansion;
    }

    @Nonnull
    public String[] getUsePath() {
        return usePath;
    }

    @Nullable
    public String getAlias() {
        return alias;
    }

    @Nonnull
    public VisibilityLight getVisibility() {
        return visibility;
    }

    public boolean isGlob() {
        return isGlob;
    }

    public boolean isExternCrate() {
        return isExternCrate;
    }

    public boolean isPrelude() {
        return isPrelude;
    }

    public boolean isDeeplyEnabledByCfg() {
        return isDeeplyEnabledByCfg;
    }

    public boolean isMacroUse() {
        return isMacroUse;
    }

    public int getOffsetInExpansion() {
        return offsetInExpansion;
    }
}
