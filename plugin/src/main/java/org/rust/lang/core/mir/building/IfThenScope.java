/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.lang.core.mir.building;

import jakarta.annotation.Nonnull;
import org.rust.lang.core.types.regions.Scope;

public class IfThenScope {
    @Nonnull
    private final Scope scope;
    @Nonnull
    private final DropTree elseDrops;

    public IfThenScope(@Nonnull Scope scope, @Nonnull DropTree elseDrops) {
        this.scope = scope;
        this.elseDrops = elseDrops;
    }

    @Nonnull
    public Scope getScope() {
        return scope;
    }

    @Nonnull
    public DropTree getElseDrops() {
        return elseDrops;
    }
}
