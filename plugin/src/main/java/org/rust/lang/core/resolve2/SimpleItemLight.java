/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.lang.core.resolve2;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import org.rust.lang.core.resolve.Namespace;

import java.io.DataOutput;
import java.io.IOException;
import java.util.Set;
import org.rust.lang.core.psi.RsProcMacroKind;

/**
 * Lightweight representation of simple items (functions, constants, type aliases, etc.)
 */
public class SimpleItemLight extends ItemLight {

    private final boolean isTrait;
    @Nullable
    private final org.rust.lang.core.psi.RsProcMacroKind procMacroKind;

    public SimpleItemLight(
        @Nonnull String name,
        @Nonnull VisibilityLight visibility,
        boolean isDeeplyEnabledByCfg,
        @Nonnull Set<Namespace> namespaces
    ) {
        this(name, visibility, isDeeplyEnabledByCfg, namespaces, false, null);
    }

    public SimpleItemLight(
        @Nonnull String name,
        @Nonnull VisibilityLight visibility,
        boolean isDeeplyEnabledByCfg,
        @Nonnull Set<Namespace> namespaces,
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
