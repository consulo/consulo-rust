/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.lang.core.resolve2;

import org.jetbrains.annotations.NotNull;

import java.util.Objects;

public final class ResolvePathResult {
    @NotNull
    private final PerNs resolvedDef;
    private final boolean reachedFixedPoint;
    private final boolean visitedOtherCrate;

    public ResolvePathResult(@NotNull PerNs resolvedDef, boolean reachedFixedPoint, boolean visitedOtherCrate) {
        this.resolvedDef = resolvedDef;
        this.reachedFixedPoint = reachedFixedPoint;
        this.visitedOtherCrate = visitedOtherCrate;
    }

    @NotNull
    public PerNs getResolvedDef() {
        return resolvedDef;
    }

    public boolean isReachedFixedPoint() {
        return reachedFixedPoint;
    }

    public boolean isVisitedOtherCrate() {
        return visitedOtherCrate;
    }

    @NotNull
    public static ResolvePathResult empty(boolean reachedFixedPoint) {
        return new ResolvePathResult(PerNs.Empty, reachedFixedPoint, false);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof ResolvePathResult)) return false;
        ResolvePathResult that = (ResolvePathResult) o;
        return reachedFixedPoint == that.reachedFixedPoint
            && visitedOtherCrate == that.visitedOtherCrate
            && Objects.equals(resolvedDef, that.resolvedDef);
    }

    @Override
    public int hashCode() {
        return Objects.hash(resolvedDef, reachedFixedPoint, visitedOtherCrate);
    }
}
