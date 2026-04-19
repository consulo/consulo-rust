/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.lang.core.resolve2;

import com.intellij.openapi.util.ModificationTracker;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
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
    @NotNull
    private final ModificationTracker structureModificationTracker;

    @Nullable
    private volatile CrateDefMap defMap;

    @NotNull
    private final AtomicLong defMapStamp = new AtomicLong(-1);

    private volatile boolean shouldRebuild = true;
    private volatile boolean shouldRecheck = false;
    @NotNull
    private final Set<RsFile> changedFiles = new HashSet<>();

    public DefMapHolder(int crateId, @NotNull ModificationTracker structureModificationTracker) {
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

    @NotNull
    public Set<RsFile> getChangedFiles() {
        return changedFiles;
    }

    public void addChangedFile(@NotNull RsFile file) {
        changedFiles.add(file);
        defMapStamp.decrementAndGet();
    }

    public void setDefMap(@Nullable CrateDefMap defMap) {
        this.defMap = defMap;
        shouldRebuild = false;
        setLatestStamp();
    }

    public boolean updateShouldRebuild(@NotNull Crate crate) {
        boolean shouldRebuild = FacadeMetaInfo.getShouldRebuild(this, crate);
        if (shouldRebuild) {
            setShouldRebuild(true);
        } else {
            setLatestStamp();
        }
        return shouldRebuild;
    }

    @Override
    @NotNull
    public String toString() {
        return "DefMapHolder(" + defMap + ", stamp=" + defMapStamp + ")";
    }
}
