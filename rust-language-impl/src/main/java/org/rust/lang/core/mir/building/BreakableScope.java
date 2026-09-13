/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.lang.core.mir.building;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import org.rust.lang.core.mir.schemas.MirPlace;
import org.rust.lang.core.types.regions.Scope;

public class BreakableScope {
    @Nonnull
    private final Scope scope;
    @Nonnull
    private final MirPlace breakDestination;
    @Nonnull
    private final DropTree breakDrops;
    @Nullable
    private final DropTree continueDrops;

    public BreakableScope(
        @Nonnull Scope scope,
        @Nonnull MirPlace breakDestination,
        @Nonnull DropTree breakDrops,
        @Nullable DropTree continueDrops
    ) {
        this.scope = scope;
        this.breakDestination = breakDestination;
        this.breakDrops = breakDrops;
        this.continueDrops = continueDrops;
    }

    @Nonnull
    public Scope getScope() {
        return scope;
    }

    @Nonnull
    public MirPlace getBreakDestination() {
        return breakDestination;
    }

    @Nonnull
    public DropTree getBreakDrops() {
        return breakDrops;
    }

    @Nullable
    public DropTree getContinueDrops() {
        return continueDrops;
    }
}
