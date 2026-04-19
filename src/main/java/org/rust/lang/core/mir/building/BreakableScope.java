/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.lang.core.mir.building;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.rust.lang.core.mir.schemas.MirPlace;
import org.rust.lang.core.types.regions.Scope;

public class BreakableScope {
    @NotNull
    private final Scope scope;
    @NotNull
    private final MirPlace breakDestination;
    @NotNull
    private final DropTree breakDrops;
    @Nullable
    private final DropTree continueDrops;

    public BreakableScope(
        @NotNull Scope scope,
        @NotNull MirPlace breakDestination,
        @NotNull DropTree breakDrops,
        @Nullable DropTree continueDrops
    ) {
        this.scope = scope;
        this.breakDestination = breakDestination;
        this.breakDrops = breakDrops;
        this.continueDrops = continueDrops;
    }

    @NotNull
    public Scope getScope() {
        return scope;
    }

    @NotNull
    public MirPlace getBreakDestination() {
        return breakDestination;
    }

    @NotNull
    public DropTree getBreakDrops() {
        return breakDrops;
    }

    @Nullable
    public DropTree getContinueDrops() {
        return continueDrops;
    }
}
