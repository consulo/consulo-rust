/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.lang.core.types.infer;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import org.rust.lang.core.types.ty.Ty;
import org.rust.lang.core.types.ty.TyProjection;
import org.rust.lang.utils.snapshot.Snapshot;
import org.rust.lang.utils.snapshot.SnapshotMap;

public class ProjectionCache {
    private final SnapshotMap<TyProjection, ProjectionCacheEntry> myMap = new SnapshotMap<>();

    @Nonnull
    public Snapshot startSnapshot() {
        return myMap.startSnapshot();
    }

    @Nullable
    public ProjectionCacheEntry tryStart(@Nonnull TyProjection key) {
        ProjectionCacheEntry existing = myMap.get(key);
        if (existing != null) return existing;
        myMap.put(key, ProjectionCacheEntry.InProgress.INSTANCE);
        return null;
    }

    private void put(@Nonnull TyProjection key, @Nonnull ProjectionCacheEntry value) {
        ProjectionCacheEntry prev = myMap.put(key, value);
        if (prev == null) throw new IllegalStateException("never started projecting for `" + key + "`");
    }

    public void putTy(@Nonnull TyProjection key, @Nonnull TyWithObligations<Ty> value) {
        put(key, new ProjectionCacheEntry.NormalizedTy(value));
    }

    public void ambiguous(@Nonnull TyProjection key) {
        put(key, ProjectionCacheEntry.Ambiguous.INSTANCE);
    }

    public void error(@Nonnull TyProjection key) {
        put(key, ProjectionCacheEntry.Error.INSTANCE);
    }
}
