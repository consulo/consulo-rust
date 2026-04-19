/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.lang.core.mir.building;

import org.jetbrains.annotations.NotNull;
import org.rust.lang.core.types.regions.Scope;

public class IfThenScope {
    @NotNull
    private final Scope scope;
    @NotNull
    private final DropTree elseDrops;

    public IfThenScope(@NotNull Scope scope, @NotNull DropTree elseDrops) {
        this.scope = scope;
        this.elseDrops = elseDrops;
    }

    @NotNull
    public Scope getScope() {
        return scope;
    }

    @NotNull
    public DropTree getElseDrops() {
        return elseDrops;
    }
}
