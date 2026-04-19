/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.lang.core.resolve2;

import com.google.common.util.concurrent.SettableFuture;
import com.intellij.concurrency.SensitiveProgressWrapper;
import com.intellij.openapi.progress.ProgressIndicator;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.rust.lang.core.crate.Crate;
import org.rust.openapiext.OpenApiUtil;
import org.rust.stdext.CollectionsUtil;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.atomic.AtomicInteger;
import org.rust.lang.core.resolve2.FacadeBuildDefMap;

/**
 * Builds CrateDefMap for crates in parallel using pool and with respect to dependency graph.
 */
public class DefMapsBuilder {
    @NotNull
    private final DefMapService defMapService;
    @NotNull
    private final List<Crate> crates;
    @NotNull
    private final ProgressIndicator indicator;
    @Nullable
    private final ExecutorService pool;
    @Nullable
    private final ExecutorService poolForMacros;

    @NotNull
    private final Map<Crate, AtomicInteger> remainingDependenciesCounts;
    @NotNull
    private final Map<Crate, CrateDefMap> builtDefMaps;

    @NotNull
    private final AtomicInteger remainingNumberCrates;
    @NotNull
    private final SettableFuture<Void> future = SettableFuture.create();

    @NotNull
    private final Map<Crate, Long> tasksTimes = new ConcurrentHashMap<>();

    public DefMapsBuilder(
        @NotNull DefMapService defMapService,
        @NotNull List<Crate> crates,
        @NotNull Map<Crate, CrateDefMap> defMaps,
        @NotNull ProgressIndicator indicator,
        @Nullable ExecutorService pool,
        @Nullable ExecutorService poolForMacros
    ) {
        if (crates.isEmpty()) throw new IllegalArgumentException("crates must not be empty");

        this.defMapService = defMapService;
        this.crates = crates;
        this.indicator = indicator;
        this.pool = pool;
        this.poolForMacros = poolForMacros;
        this.builtDefMaps = new ConcurrentHashMap<>(defMaps);
        this.remainingNumberCrates = new AtomicInteger(crates.size());

        Set<Crate> cratesSet = new HashSet<>(crates);
        this.remainingDependenciesCounts = new HashMap<>();
        for (Crate crate : crates) {
            int remaining = 0;
            for (Crate.Dependency dep : crate.getDependencies()) {
                if (cratesSet.contains(dep.getCrate())) {
                    remaining++;
                }
            }
            remainingDependenciesCounts.put(crate, new AtomicInteger(remaining));
        }
    }

    public void build() {
        long startTime = System.currentTimeMillis();
        if (pool != null) {
            buildAsync();
        } else {
            buildSync();
        }
        long wallTime = System.currentTimeMillis() - startTime;
        printTimeStatistics(wallTime);
    }

    private void buildAsync() {
        for (Map.Entry<Crate, AtomicInteger> entry : remainingDependenciesCounts.entrySet()) {
            if (entry.getValue().get() == 0) {
                buildDefMapAsync(entry.getKey());
            }
        }
        try {
            future.get();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private void buildSync() {
        for (Crate crate : crates) {
            long start = System.currentTimeMillis();
            doBuildDefMap(crate);
            tasksTimes.put(crate, System.currentTimeMillis() - start);
        }
    }

    private void buildDefMapAsync(@NotNull Crate crate) {
        if (pool == null) throw new IllegalStateException("pool is null");
        pool.execute(() -> {
            try {
                long start = System.currentTimeMillis();
                OpenApiUtil.computeInReadActionWithWriteActionPriority(new SensitiveProgressWrapper(indicator), () -> {
                    doBuildDefMap(crate);
                    return null;
                });
                tasksTimes.put(crate, System.currentTimeMillis() - start);
            } catch (Throwable e) {
                future.setException(e);
                return;
            }
            onCrateFinished(crate);
        });
    }

    private void doBuildDefMap(@NotNull Crate crate) {
        Integer crateId = crate.getId();
        if (crateId == null) return;

        Map<Crate, CrateDefMap> allDependenciesDefMaps = new HashMap<>();
        for (Crate dep : crate.getFlatDependencies()) {
            CrateDefMap depDefMap = builtDefMaps.get(dep);
            if (depDefMap != null && dep.getId() != null) {
                allDependenciesDefMaps.put(dep, depDefMap);
            }
        }
        CrateDefMap defMap = FacadeBuildDefMap.buildDefMap(
            crate, allDependenciesDefMaps, poolForMacros, indicator, true
        );
        defMapService.setDefMap(crateId, defMap);
        if (defMap != null) {
            builtDefMaps.put(crate, defMap);
        }
    }

    private void onCrateFinished(@NotNull Crate crate) {
        if (future.isDone()) return;
        for (Crate revDep : crate.getReverseDependencies()) {
            onDependencyCrateFinished(revDep);
        }
        if (remainingNumberCrates.decrementAndGet() == 0) {
            future.set(null);
        }
    }

    private void onDependencyCrateFinished(@NotNull Crate crate) {
        AtomicInteger count = remainingDependenciesCounts.get(crate);
        if (count == null) return;
        if (count.decrementAndGet() == 0) {
            buildDefMapAsync(crate);
        }
    }

    private void printTimeStatistics(long wallTime) {
        if (!CrateDefMap.RESOLVE_LOG.isDebugEnabled()) return;
        long totalTime = 0;
        for (long t : tasksTimes.values()) totalTime += t;
        CrateDefMap.RESOLVE_LOG.debug("DefMapsBuilder wallTime: " + wallTime + "ms, totalTime: " + totalTime + "ms");
    }
}
