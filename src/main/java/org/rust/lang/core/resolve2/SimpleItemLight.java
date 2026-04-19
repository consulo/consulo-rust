/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.lang.core.resolve2;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.rust.lang.core.resolve.Namespace;

import java.io.DataOutput;
import java.io.IOException;
import java.util.Set;

/**
 * Lightweight representation of simple items (functions, constants, type aliases, etc.)
 */
public class SimpleItemLight extends ItemLight {

    private final boolean isTrait;
    @Nullable
    private final org.rust.lang.core.psi.RsProcMacroKind procMacroKind;

    public SimpleItemLight(
        @NotNull String name,
        @NotNull VisibilityLight visibility,
        boolean isDeeplyEnabledByCfg,
        @NotNull Set<Namespace> namespaces
    ) {
        this(name, visibility, isDeeplyEnabledByCfg, namespaces, false, null);
    }

    public SimpleItemLight(
        @NotNull String name,
        @NotNull VisibilityLight visibility,
        boolean isDeeplyEnabledByCfg,
        @NotNull Set<Namespace> namespaces,
        boolean isTrait,
        @Nullable org.rust.lang.core.psi.RsProcMacroKind procMacroKind
    ) {
        super(name, visibility, isDeeplyEnabledByCfg, namespaces);
        this.isTrait = isTrait;
        this.procMacroKind = procMacroKind;
    }

    public boolean isTrait() {
        return isTrait;
    }

    @Nullable
    public org.rust.lang.core.psi.RsProcMacroKind getProcMacroKind() {
        return procMacroKind;
    }
}
