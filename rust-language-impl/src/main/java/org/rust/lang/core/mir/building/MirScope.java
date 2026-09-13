/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.lang.core.mir.building;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import org.rust.lang.core.mir.schemas.MirSourceScope;
import org.rust.lang.core.types.regions.Scope;

import java.util.*;

public class MirScope {
    @Nonnull
    private final MirSourceScope sourceScope;
    @Nonnull
    private final Scope regionScope;
    @Nonnull
    private final List<Drop> drops = new ArrayList<>();
    @Nullable
    private DropTree.DropNode cachedUnwindDrop;

    public MirScope(@Nonnull MirSourceScope sourceScope, @Nonnull Scope regionScope) {
        this.sourceScope = sourceScope;
        this.regionScope = regionScope;
    }

    @Nonnull
    public MirSourceScope getSourceScope() {
        return sourceScope;
    }

    @Nonnull
    public Scope getRegionScope() {
        return regionScope;
    }

    @Nullable
    public DropTree.DropNode getCachedUnwindDrop() {
        return cachedUnwindDrop;
    }

    public void setCachedUnwindDrop(@Nonnull DropTree.DropNode dropNode) {
        this.cachedUnwindDrop = dropNode;
    }

    @Nonnull
    public Iterator<Drop> reversedDrops() {
        List<Drop> reversed = new ArrayList<>(drops);
        Collections.reverse(reversed);
        return reversed.iterator();
    }

    @Nonnull
    public List<Drop> drops() {
        return Collections.unmodifiableList(drops);
    }

    public void addDrop(@Nonnull Drop drop) {
        drops.add(drop);
    }

    public void invalidateCaches() {
        cachedUnwindDrop = null;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        MirScope mirScope = (MirScope) o;
        return Objects.equals(sourceScope, mirScope.sourceScope) && Objects.equals(regionScope, mirScope.regionScope);
    }

    @Override
    public int hashCode() {
        return Objects.hash(sourceScope, regionScope);
    }

    @Override
    public String toString() {
        return "MirScope(sourceScope=" + sourceScope + ", regionScope=" + regionScope + ")";
    }
}
