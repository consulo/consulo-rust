/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.lang.core.resolve2;

import com.google.common.util.concurrent.SettableFuture;
import com.intellij.concurrency.SensitiveProgressWrapper;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.application.ReadAction;
import com.intellij.openapi.progress.EmptyProgressIndicator;
import com.intellij.openapi.progress.ProcessCanceledException;
import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.progress.ProgressManager;
import com.intellij.openapi.project.DumbService;
import com.intellij.openapi.project.Project;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
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
    public static CrateDefMap getOrUpdateIfNeeded(@NotNull DefMapService service, int crateId) {
        Map<Integer, CrateDefMap> result = getOrUpdateIfNeeded(service, Collections.singletonList(crateId));
        return result.get(crateId);
    }

    @NotNull
    public static Map<Integer, CrateDefMap> getOrUpdateIfNeeded(
        @NotNull DefMapService service,
        @NotNull List<Integer> crates
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

    @NotNull
    private static Map<Integer, CrateDefMap> defMaps(@NotNull List<DefMapHolder> holders) {
        Map<Integer, CrateDefMap> result = new HashMap<>();
        for (DefMapHolder holder : holders) {
            result.put(holder.getCrateId(), holder.getDefMap());
        }
        return result;
    }

    /**
     * Called from macro expansion task.
     */
    @NotNull
    public static List<CrateDefMap> updateDefMapForAllCratesWithWriteActionPriority(
        @NotNull DefMapService service,
        @NotNull ProgressIndicator indicator,
        boolean multithread
    ) {
        return OpenApiUtil.executeUnderProgressWithWriteActionPriorityWithRetries(indicator, wrappedIndicator ->
            doUpdateDefMapForAllCrates(service, wrappedIndicator, multithread, null)
        );
    }

    @NotNull
    public static List<CrateDefMap> updateDefMapForAllCrates(@NotNull DefMapService service) {
        return updateDefMapForAllCrates(service, true);
    }

    @NotNull
    public static List<CrateDefMap> updateDefMapForAllCrates(@NotNull DefMapService service, boolean multithread) {
        ProgressIndicator progressIndicator = ProgressManager.getGlobalProgressIndicator();
        if (progressIndicator == null) progressIndicator = new EmptyProgressIndicator();
        return doUpdateDefMapForAllCrates(service, progressIndicator, multithread, null);
    }

    @NotNull
    private static List<CrateDefMap> doUpdateDefMapForAllCrates(
        @NotNull DefMapService service,
        @NotNull ProgressIndicator indicator,
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
    public static void forceRebuildDefMapForAllCrates(@NotNull Project project, boolean multithread) {
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
    public static void forceRebuildDefMapForCrate(@NotNull Project project, int crateId) {
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
    @NotNull
    public static List<CrateDefMap> getAllDefMaps(@NotNull Project project) {
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
        @NotNull private final DefMapService defMapService;
        @NotNull private final ProgressIndicator indicator;
        private final boolean multithread;
        @NotNull private final ExecutorService pool;
        @NotNull private final List<Crate> topSortedCrates;
        @NotNull private final Collection<Crate> crates;
        private int numberUpdatedCrates = 0;

        DefMapUpdater(
            @Nullable List<Integer> rootCrateIds,
            @NotNull DefMapService defMapService,
            @NotNull ProgressIndicator indicator,
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
                for (int id : rootCrateIds) {
                    for (Crate c : topSortedCrates) {
                        if (id == c.getId()) {
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

        @NotNull
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

        @NotNull
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

        @NotNull
        private List<Crate> findCratesToUpdate(@NotNull List<CratePair> cratesToCheck) {
            List<Crate> result = new ArrayList<>();
            for (CratePair pair : cratesToCheck) {
                if (pair.holder.updateShouldRebuild(pair.crate)) {
                    result.add(pair.crate);
                }
            }
            return result;
        }

        @NotNull
        private Set<Crate> getCratesToUpdateWithReversedDependencies(@NotNull List<Crate> cratesToUpdate) {
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

        @NotNull
        private Map<Crate, CrateDefMap> getBuiltDefMaps(@NotNull Set<Crate> cratesToUpdateAll) {
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

        @NotNull
        private List<Crate> topSort(@NotNull Set<Crate> crateSet) {
            if (crateSet.size() <= 1) return new ArrayList<>(crateSet);
            List<Crate> result = new ArrayList<>();
            for (Crate c : topSortedCrates) {
                if (crateSet.contains(c)) result.add(c);
            }
            return result;
        }
    }

    private static class CratePair {
        @NotNull final Crate crate;
        @NotNull final DefMapHolder holder;

        CratePair(@NotNull Crate crate, @NotNull DefMapHolder holder) {
            this.crate = crate;
            this.holder = holder;
        }
    }

    @NotNull
    private static Set<Crate> withReversedDependencies(@NotNull List<Crate> crates) {
        Set<Crate> result = new HashSet<>();
        for (Crate crate : crates) {
            addReverseDeps(crate, result);
        }
        return result;
    }

    private static void addReverseDeps(@NotNull Crate crate, @NotNull Set<Crate> result) {
        if (crate.getId() == null || !result.add(crate)) return;
        for (Crate reverseDep : crate.getReverseDependencies()) {
            addReverseDeps(reverseDep, result);
        }
    }
}
