/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.lang.core.types.infer;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.rust.lang.core.types.ty.Ty;
import org.rust.lang.core.types.ty.TyProjection;
import org.rust.lang.utils.snapshot.Snapshot;
import org.rust.lang.utils.snapshot.SnapshotMap;

public class ProjectionCache {
    private final SnapshotMap<TyProjection, ProjectionCacheEntry> myMap = new SnapshotMap<>();

    @NotNull
    public Snapshot startSnapshot() {
        return myMap.startSnapshot();
    }

    @Nullable
    public ProjectionCacheEntry tryStart(@NotNull TyProjection key) {
        ProjectionCacheEntry existing = myMap.get(key);
        if (existing != null) return existing;
        myMap.put(key, ProjectionCacheEntry.InProgress.INSTANCE);
        return null;
    }

    private void put(@NotNull TyProjection key, @NotNull ProjectionCacheEntry value) {
        ProjectionCacheEntry prev = myMap.put(key, value);
        if (prev == null) throw new IllegalStateException("never started projecting for `" + key + "`");
    }

    public void putTy(@NotNull TyProjection key, @NotNull TyWithObligations<Ty> value) {
        put(key, new ProjectionCacheEntry.NormalizedTy(value));
    }

    public void ambiguous(@NotNull TyProjection key) {
        put(key, ProjectionCacheEntry.Ambiguous.INSTANCE);
    }

    public void error(@NotNull TyProjection key) {
        put(key, ProjectionCacheEntry.Error.INSTANCE);
    }
}
