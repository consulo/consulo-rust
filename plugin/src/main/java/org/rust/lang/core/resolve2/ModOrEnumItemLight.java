/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.lang.core.resolve2;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import org.rust.lang.core.resolve.Namespace;

import java.util.Set;

/**
 * Lightweight representation of mod or enum items.
 */
public class ModOrEnumItemLight extends ItemLight {

    private final int macroIndexInParent;
    @Nullable
    private final String pathAttribute;
    private final boolean hasMacroUse;

    public ModOrEnumItemLight(
        @Nonnull String name,
        @Nonnull VisibilityLight visibility,
        boolean isDeeplyEnabledByCfg,
        @Nonnull Set<Namespace> namespaces
    ) {
        this(name, visibility, isDeeplyEnabledByCfg, namespaces, 0, null, false);
    }

    public ModOrEnumItemLight(
        @Nonnull String name,
        @Nonnull VisibilityLight visibility,
        boolean isDeeplyEnabledByCfg,
        @Nonnull Set<Namespace> namespaces,
        int macroIndexInParent,
        @Nullable String pathAttribute,
        boolean hasMacroUse
    ) {
        super(name, visibility, isDeeplyEnabledByCfg, namespaces);
        this.macroIndexInParent = macroIndexInParent;
        this.pathAttribute = pathAttribute;
        this.hasMacroUse = hasMacroUse;
    }

    public int getMacroIndexInParent() {
        return macroIndexInParent;
    }

    @Nullable
    public String getPathAttribute() {
        return pathAttribute;
    }

    public boolean isHasMacroUse() {
        return hasMacroUse;
    }
}
