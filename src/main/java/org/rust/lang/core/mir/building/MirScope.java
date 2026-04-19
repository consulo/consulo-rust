/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.lang.core.mir.building;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.rust.lang.core.mir.schemas.MirSourceScope;
import org.rust.lang.core.types.regions.Scope;

import java.util.*;

public class MirScope {
    @NotNull
    private final MirSourceScope sourceScope;
    @NotNull
    private final Scope regionScope;
    @NotNull
    private final List<Drop> drops = new ArrayList<>();
    @Nullable
    private DropTree.DropNode cachedUnwindDrop;

    public MirScope(@NotNull MirSourceScope sourceScope, @NotNull Scope regionScope) {
        this.sourceScope = sourceScope;
        this.regionScope = regionScope;
    }

    @NotNull
    public MirSourceScope getSourceScope() {
        return sourceScope;
    }

    @NotNull
    public Scope getRegionScope() {
        return regionScope;
    }

    @Nullable
    public DropTree.DropNode getCachedUnwindDrop() {
        return cachedUnwindDrop;
    }

    public void setCachedUnwindDrop(@NotNull DropTree.DropNode dropNode) {
        this.cachedUnwindDrop = dropNode;
    }

    @NotNull
    public Iterator<Drop> reversedDrops() {
        List<Drop> reversed = new ArrayList<>(drops);
        Collections.reverse(reversed);
        return reversed.iterator();
    }

    @NotNull
    public List<Drop> drops() {
        return Collections.unmodifiableList(drops);
    }

    public void addDrop(@NotNull Drop drop) {
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
