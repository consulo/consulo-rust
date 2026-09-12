/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.lang.core.resolve2;

import com.google.common.util.concurrent.SettableFuture;
import consulo.application.ApplicationManager;
import consulo.application.ReadAction;
import consulo.application.progress.EmptyProgressIndicator;
import consulo.component.ProcessCanceledException;
import consulo.application.progress.ProgressIndicator;
import consulo.application.progress.ProgressManager;
import consulo.project.DumbService;
import consulo.project.Project;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import org.rust.lang.core.crate.Crate;
import org.rust.lang.core.crate.CratePersistentId;
import org.rust.lang.core.crate.CrateGraphService;
import org.rust.lang.core.macros.MacroExpansionSharedCache;
import org.rust.openapiext.OpenApiUtil;
import org.rust.stdext.CollectionsUtil;

import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Provides functions for updating and rebuilding CrateDefMap.
 */
public final class FacadeUpdateDefMap {

    private FacadeUpdateDefMap() {}

    /**
     * Returns defMap stored in DefMapHolder if it is up-to-date.
     * Otherwise, rebuilds if needed CrateDefMap for crate and all its dependencies.
     */
    @Nullable
    public static CrateDefMap getOrUpdateIfNeeded(@Nonnull DefMapService service, int crateId) {
        Map<Integer, CrateDefMap> result = getOrUpdateIfNeeded(service, Collections.singletonList(crateId));
        return result.get(crateId);
    }

    @Nonnull
    public static Map<Integer, CrateDefMap> getOrUpdateIfNeeded(
        @Nonnull DefMapService service,
        @Nonnull List<Integer> crates
    ) {
        List<DefMapHolder> holders = new ArrayList<>();
        for (int crateId : crates) {
            holders.add(service.getDefMapHolder(crateId));
        }

        boolean allUpToDate = true;
        for (DefMapHolder holder : holders) {
            if (!holder.hasLatestStamp()) {
                allUpToDate = false;
                break;
            }
        }
        if (allUpToDate) return defMaps(holders);

        OpenApiUtil.checkReadAccessAllowed();
        OpenApiUtil.checkIsSmartMode(service.getProject());

        service.getDefMapsBuildLock().lock();
        try {
            if (service.getDefMapsBuildLock().getHoldCount() != 1) {
                throw new IllegalStateException("Can't use resolve while building CrateDefMap");
            }
            allUpToDate = true;
            for (DefMapHolder holder : holders) {
                if (!holder.hasLatestStamp()) {
                    allUpToDate = false;
                    break;
                }
            }
            if (allUpToDate) return defMaps(holders);

            try {
                ProgressIndicator indicator = ProgressManager.getGlobalProgressIndicator();
                if (indicator == null) indicator = new EmptyProgressIndicator();
                new DefMapUpdater(crates, service, indicator, true).run();
                for (DefMapHolder holder : holders) {
                    if (service.hasDefMapFor(holder.getCrateId())) {
                        holder.checkHasLatestStamp();
                    }
                }
                return defMaps(holders);
            } finally {
                MacroExpansionSharedCache.getInstance().flush();
            }
        } finally {
            service.getDefMapsBuildLock().unlock();
        }
    }

    @Nonnull
    private static Map<Integer, CrateDefMap> defMaps(@Nonnull List<DefMapHolder> holders) {
        Map<Integer, CrateDefMap> result = new HashMap<>();
        for (DefMapHolder holder : holders) {
            result.put(holder.getCrateId(), holder.getDefMap());
        }
        return result;
    }

    /**
     * Called from macro expansion task.
     */
    @Nonnull
    public static List<CrateDefMap> updateDefMapForAllCratesWithWriteActionPriority(
        @Nonnull DefMapService service,
        @Nonnull ProgressIndicator indicator,
        boolean multithread
    ) {
        return OpenApiUtil.executeUnderProgressWithWriteActionPriorityWithRetries(indicator, wrappedIndicator ->
            doUpdateDefMapForAllCrates(service, wrappedIndicator, multithread, null)
        );
    }

    @Nonnull
    public static List<CrateDefMap> updateDefMapForAllCrates(@Nonnull DefMapService service) {
        return updateDefMapForAllCrates(service, true);
    }

    @Nonnull
    public static List<CrateDefMap> updateDefMapForAllCrates(@Nonnull DefMapService service, boolean multithread) {
        ProgressIndicator progressIndicator = ProgressManager.getGlobalProgressIndicator();
        if (progressIndicator == null) progressIndicator = new EmptyProgressIndicator();
        return doUpdateDefMapForAllCrates(service, progressIndicator, multithread, null);
    }

    @Nonnull
    private static List<CrateDefMap> doUpdateDefMapForAllCrates(
        @Nonnull DefMapService service,
        @Nonnull ProgressIndicator indicator,
        boolean multithread,
        @Nullable List<Integer> rootCrateIds
    ) {
        DumbService dumbService = DumbService.getInstance(service.getProject());
        return OpenApiUtil.runReadActionInSmartMode(dumbService, () -> {
            service.getDefMapsBuildLock().lock();
            try {
                if (service.getDefMapsBuildLock().getHoldCount() != 1) {
                    throw new IllegalStateException("Lock held more than once");
                }
                List<CrateDefMap> result = new DefMapUpdater(rootCrateIds, service, indicator, multithread).run();
                if (rootCrateIds == null) {
                    service.setAllDefMapsUpToDate();
                }
                return result;
            } finally {
                service.getDefMapsBuildLock().unlock();
            }
        });
    }

    /**
     * Force rebuild of DefMap for all crates.
     */
    public static void forceRebuildDefMapForAllCrates(@Nonnull Project project, boolean multithread) {
        DefMapService defMapService = project.getService(DefMapService.class);
        ReadAction.run(() -> {
            defMapService.getDefMapsBuildLock().lock();
            try {
                defMapService.scheduleRebuildAllDefMaps();
            } finally {
                defMapService.getDefMapsBuildLock().unlock();
            }
        });
        doUpdateDefMapForAllCrates(defMapService, new EmptyProgressIndicator(), multithread, null);
    }

    /**
     * Force rebuild of DefMap for a specific crate.
     */
    public static void forceRebuildDefMapForCrate(@Nonnull Project project, int crateId) {
        DefMapService defMapService = project.getService(DefMapService.class);
        ReadAction.run(() -> {
            defMapService.getDefMapsBuildLock().lock();
            try {
                defMapService.scheduleRebuildDefMap(crateId);
            } finally {
                defMapService.getDefMapsBuildLock().unlock();
            }
        });
        doUpdateDefMapForAllCrates(defMapService, new EmptyProgressIndicator(), false, Collections.singletonList(crateId));
    }

    /**
     * Gets all DefMaps for the project.
     */
    @Nonnull
    public static List<CrateDefMap> getAllDefMaps(@Nonnull Project project) {
        DefMapService service = project.getService(DefMapService.class);
        List<CrateDefMap> result = new ArrayList<>();
        for (Crate crate : CrateGraphService.crateGraph(project).getTopSortedCrates()) {
            Integer id = crate.getId();
            if (id == null) continue;
            CrateDefMap defMap = getOrUpdateIfNeeded(service, id);
            if (defMap != null) result.add(defMap);
        }
        return result;
    }

    /**
     * Internal updater that rebuilds def maps for a set of crates.
     */
    private static class DefMapUpdater {
        @Nullable private final List<Integer> rootCrateIds;
        @Nonnull private final DefMapService defMapService;
        @Nonnull private final ProgressIndicator indicator;
        private final boolean multithread;
        @Nonnull private final ExecutorService pool;
        @Nonnull private final List<Crate> topSortedCrates;
        @Nonnull private final Collection<Crate> crates;
        private int numberUpdatedCrates = 0;

        DefMapUpdater(
            @Nullable List<Integer> rootCrateIds,
            @Nonnull DefMapService defMapService,
            @Nonnull ProgressIndicator indicator,
            boolean multithread
        ) {
            this.rootCrateIds = rootCrateIds;
            this.defMapService = defMapService;
            this.indicator = indicator;
            this.multithread = multithread && !ApplicationManager.getApplication().isWriteAccessAllowed();
            this.pool = ResolveCommonThreadPool.get();
            this.topSortedCrates = CrateGraphService.crateGraph(defMapService.getProject()).getTopSortedCrates();

            if (rootCrateIds == null) {
                this.crates = topSortedCrates;
            } else {
                List<Crate> rootCrates = new ArrayList<>();
                for (Integer id : rootCrateIds) {
                    for (Crate c : topSortedCrates) {
                        // Compared as boxed values: a crate whose target has no root has a null id, and
                        // unboxing it here would throw rather than skip the crate.
                        if (id != null && id.equals(c.getId())) {
                            rootCrates.add(c);
                            break;
                        }
                    }
                }
                Set<Crate> crateSet = new HashSet<>();
                for (Crate root : rootCrates) {
                    crateSet.addAll(root.getFlatDependencies());
                    crateSet.add(root);
                }
                this.crates = topSort(crateSet);
            }
        }

        @Nonnull
        List<CrateDefMap> run() {
            OpenApiUtil.checkReadAccessAllowed();
            long start = System.currentTimeMillis();
            OpenApiUtil.executeUnderProgress(indicator, () -> { doRun(); return null; });
            long time = System.currentTimeMillis() - start;
            if (numberUpdatedCrates > 0) {
                String cratesCount = numberUpdatedCrates == topSortedCrates.size() ? "all" : String.valueOf(numberUpdatedCrates);
                CrateDefMap.RESOLVE_LOG.info("Updated " + cratesCount + " DefMaps in " + time + " ms");
            }
            List<CrateDefMap> result = new ArrayList<>();
            for (Crate crate : crates) {
                Integer crateId = crate.getId();
                if (crateId == null) continue;
                CrateDefMap defMap = defMapService.getDefMapHolder(crateId).getDefMap();
                if (defMap != null) result.add(defMap);
            }
            return result;
        }

        private void doRun() {
            indicator.checkCanceled();

            List<CratePair> cratesToCheck = findCratesToCheck();
            List<Crate> cratesToUpdate = findCratesToUpdate(cratesToCheck);

            defMapService.removeStaleDefMaps(topSortedCrates);
            if (cratesToUpdate.isEmpty()) return;

            Set<Crate> cratesToUpdateAll = getCratesToUpdateWithReversedDependencies(cratesToUpdate);
            Map<Crate, CrateDefMap> builtDefMaps = getBuiltDefMaps(cratesToUpdateAll);

            List<Crate> cratesToUpdateAllSorted = topSort(cratesToUpdateAll);
            ExecutorService poolForBuild = this.multithread && cratesToUpdateAllSorted.size() > 1 ? pool : null;
            ExecutorService poolForMacros = this.multithread ? pool : null;
            numberUpdatedCrates = cratesToUpdateAllSorted.size();
            new DefMapsBuilder(defMapService, cratesToUpdateAllSorted, builtDefMaps, indicator, poolForBuild, poolForMacros).build();
        }

        @Nonnull
        private List<CratePair> findCratesToCheck() {
            OpenApiUtil.checkReadAccessAllowed();
            List<CratePair> cratesToCheck = new ArrayList<>();
            for (Crate crate : crates) {
                Integer crateId = crate.getId();
                if (crateId == null) continue;
                DefMapHolder holder = defMapService.getDefMapHolder(crateId);
                if (!holder.hasLatestStamp()) {
                    cratesToCheck.add(new CratePair(crate, holder));
                }
            }
            return cratesToCheck;
        }

        @Nonnull
        private List<Crate> findCratesToUpdate(@Nonnull List<CratePair> cratesToCheck) {
            List<Crate> result = new ArrayList<>();
            for (CratePair pair : cratesToCheck) {
                if (pair.holder.updateShouldRebuild(pair.crate)) {
                    result.add(pair.crate);
                }
            }
            return result;
        }

        @Nonnull
        private Set<Crate> getCratesToUpdateWithReversedDependencies(@Nonnull List<Crate> cratesToUpdate) {
            Set<Crate> withReversedDeps = withReversedDependencies(cratesToUpdate);
            for (Crate crate : withReversedDeps) {
                Integer id = crate.getId();
                if (id == null) continue;
                DefMapHolder holder = defMapService.getDefMapHolder(id);
                holder.setShouldRebuild(true);
            }
            Set<Crate> crateSet = new HashSet<>(crates);
            Set<Crate> result = new HashSet<>();
            for (Crate c : crateSet) {
                if (withReversedDeps.contains(c)) result.add(c);
            }
            return result;
        }

        @Nonnull
        private Map<Crate, CrateDefMap> getBuiltDefMaps(@Nonnull Set<Crate> cratesToUpdateAll) {
            Map<Crate, CrateDefMap> result = new HashMap<>();
            for (Crate crate : crates) {
                if (cratesToUpdateAll.contains(crate)) continue;
                Integer crateId = crate.getId();
                if (crateId == null) continue;
                CrateDefMap defMap = defMapService.getDefMapHolder(crateId).getDefMap();
                if (defMap != null) result.put(crate, defMap);
            }
            return result;
        }

        @Nonnull
        private List<Crate> topSort(@Nonnull Set<Crate> crateSet) {
            if (crateSet.size() <= 1) return new ArrayList<>(crateSet);
            List<Crate> result = new ArrayList<>();
            for (Crate c : topSortedCrates) {
                if (crateSet.contains(c)) result.add(c);
            }
            return result;
        }
    }

    private static class CratePair {
        @Nonnull final Crate crate;
        @Nonnull final DefMapHolder holder;

        CratePair(@Nonnull Crate crate, @Nonnull DefMapHolder holder) {
            this.crate = crate;
            this.holder = holder;
        }
    }

    @Nonnull
    private static Set<Crate> withReversedDependencies(@Nonnull List<Crate> crates) {
        Set<Crate> result = new HashSet<>();
        for (Crate crate : crates) {
            addReverseDeps(crate, result);
        }
        return result;
    }

    private static void addReverseDeps(@Nonnull Crate crate, @Nonnull Set<Crate> result) {
        if (crate.getId() == null || !result.add(crate)) return;
        for (Crate reverseDep : crate.getReverseDependencies()) {
            addReverseDeps(reverseDep, result);
        }
    }
}
