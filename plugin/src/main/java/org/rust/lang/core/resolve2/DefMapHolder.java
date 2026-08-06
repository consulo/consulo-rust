/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.lang.core.resolve2;

import consulo.component.util.ModificationTracker;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import org.rust.lang.core.crate.Crate;
import org.rust.lang.core.crate.CratePersistentId;
import org.rust.lang.core.psi.RsFile;

import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.atomic.AtomicLong;
import org.rust.lang.core.resolve2.FacadeMetaInfo;

/**
 * Stores CrateDefMap and data needed to determine whether defMap is up-to-date.
 */
public class DefMapHolder {
    private final int crateId;
    @Nonnull
    private final ModificationTracker structureModificationTracker;

    @Nullable
    private volatile CrateDefMap defMap;

    @Nonnull
    private final AtomicLong defMapStamp = new AtomicLong(-1);

    private volatile boolean shouldRebuild = true;
    private volatile boolean shouldRecheck = false;
    @Nonnull
    private final Set<RsFile> changedFiles = new HashSet<>();

    public DefMapHolder(int crateId, @Nonnull ModificationTracker structureModificationTracker) {
        this.crateId = crateId;
        this.structureModificationTracker = structureModificationTracker;
    }

    public int getCrateId() {
        return crateId;
    }

    @Nullable
    public CrateDefMap getDefMap() {
        return defMap;
    }

    public boolean hasLatestStamp() {
        return defMapStamp.get() == structureModificationTracker.getModificationCount();
    }

    private void setLatestStamp() {
        defMapStamp.set(structureModificationTracker.getModificationCount());
    }

    public void checkHasLatestStamp() {
        if (defMap != null && !hasLatestStamp()) {
            CrateDefMap.RESOLVE_LOG.error(
                "DefMapHolder must have latest stamp right after DefMap(" + defMap + ") was updated. " +
                    defMapStamp + " vs " + structureModificationTracker.getModificationCount()
            );
        }
    }

    public long getModificationCount() {
        return defMapStamp.get();
    }

    public boolean isShouldRebuild() {
        return shouldRebuild;
    }

    public void setShouldRebuild(boolean value) {
        this.shouldRebuild = value;
        if (value) {
            defMapStamp.decrementAndGet();
            shouldRecheck = false;
            changedFiles.clear();
        }
    }

    public boolean isShouldRecheck() {
        return shouldRecheck;
    }

    public void setShouldRecheck(boolean value) {
        this.shouldRecheck = value;
        if (value) {
            defMapStamp.decrementAndGet();
        }
    }

    @Nonnull
    public Set<RsFile> getChangedFiles() {
        return changedFiles;
    }

    public void addChangedFile(@Nonnull RsFile file) {
        changedFiles.add(file);
        defMapStamp.decrementAndGet();
    }

    public void setDefMap(@Nullable CrateDefMap defMap) {
        this.defMap = defMap;
        shouldRebuild = false;
        setLatestStamp();
    }

    public boolean updateShouldRebuild(@Nonnull Crate crate) {
        boolean shouldRebuild = FacadeMetaInfo.getShouldRebuild(this, crate);
        if (shouldRebuild) {
            setShouldRebuild(true);
        } else {
            setLatestStamp();
        }
        return shouldRebuild;
    }

    @Override
    @Nonnull
    public String toString() {
        return "DefMapHolder(" + defMap + ", stamp=" + defMapStamp + ")";
    }
}
