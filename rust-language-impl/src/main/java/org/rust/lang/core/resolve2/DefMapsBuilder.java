/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.lang.core.resolve2;

import org.rust.stdext.CompletableFutureUtil;
import java.util.concurrent.CompletableFuture;
import org.rust.openapiext.RsSensitiveProgressWrapper;
import consulo.application.progress.ProgressIndicator;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import org.rust.lang.core.crate.Crate;
import org.rust.openapiext.OpenApiUtil;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.atomic.AtomicInteger;
import org.rust.lang.core.resolve2.FacadeBuildDefMap;

/**
 * Builds CrateDefMap for crates in parallel using pool and with respect to dependency graph.
 */
public class DefMapsBuilder {
    @Nonnull
    private final DefMapService defMapService;
    @Nonnull
    private final List<Crate> crates;
    @Nonnull
    private final ProgressIndicator indicator;
    @Nullable
    private final ExecutorService pool;
    @Nullable
    private final ExecutorService poolForMacros;

    @Nonnull
    private final Map<Crate, AtomicInteger> remainingDependenciesCounts;
    @Nonnull
    private final Map<Crate, CrateDefMap> builtDefMaps;

    @Nonnull
    private final AtomicInteger remainingNumberCrates;
    @Nonnull
    private final CompletableFuture<Void> future = new CompletableFuture<>();

    @Nonnull
    private final Map<Crate, Long> tasksTimes = new ConcurrentHashMap<>();

    public DefMapsBuilder(
        @Nonnull DefMapService defMapService,
        @Nonnull List<Crate> crates,
        @Nonnull Map<Crate, CrateDefMap> defMaps,
        @Nonnull ProgressIndicator indicator,
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
            CompletableFutureUtil.getWithRethrow(future);
        }
        catch (RuntimeException | Error e) {
            throw e;
        }
        catch (Exception e) {
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

    private void buildDefMapAsync(@Nonnull Crate crate) {
        if (pool == null) throw new IllegalStateException("pool is null");
        pool.execute(() -> {
            try {
                long start = System.currentTimeMillis();
                OpenApiUtil.computeInReadActionWithWriteActionPriority(new RsSensitiveProgressWrapper(indicator), () -> {
                    doBuildDefMap(crate);
                    return null;
                });
                tasksTimes.put(crate, System.currentTimeMillis() - start);
            } catch (Throwable e) {
                future.completeExceptionally(e);
                return;
            }
            onCrateFinished(crate);
        });
    }

    private void doBuildDefMap(@Nonnull Crate crate) {
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

    private void onCrateFinished(@Nonnull Crate crate) {
        if (future.isDone()) return;
        for (Crate revDep : crate.getReverseDependencies()) {
            onDependencyCrateFinished(revDep);
        }
        if (remainingNumberCrates.decrementAndGet() == 0) {
            future.complete(null);
        }
    }

    private void onDependencyCrateFinished(@Nonnull Crate crate) {
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
