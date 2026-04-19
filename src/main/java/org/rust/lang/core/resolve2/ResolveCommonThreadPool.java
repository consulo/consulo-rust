/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.lang.core.resolve2;

import com.intellij.openapi.Disposable;
import com.intellij.openapi.components.Service;
import org.jetbrains.annotations.NotNull;
import org.rust.openapiext.OpenApiUtil;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.ForkJoinPool.ForkJoinWorkerThreadFactory;

@Service
public final class ResolveCommonThreadPool implements Disposable {

    private static final String THREAD_NAME_PREFIX = "Rust-resolve-thread-";

    @NotNull
    private final ExecutorService pool;

    public ResolveCommonThreadPool() {
        this.pool = createPool();
    }

    @NotNull
    private ExecutorService createPool() {
        int parallelism = Runtime.getRuntime().availableProcessors();
        ForkJoinWorkerThreadFactory threadFactory = p -> {
            var thread = ForkJoinPool.defaultForkJoinWorkerThreadFactory.newThread(p);
            thread.setName(THREAD_NAME_PREFIX + thread.getPoolIndex());
            return thread;
        };
        return new ForkJoinPool(parallelism, threadFactory, null, true);
    }

    @Override
    public void dispose() {
        pool.shutdown();
    }

    @NotNull
    public static ExecutorService get() {
        return com.intellij.openapi.application.ApplicationManager.getApplication()
            .getService(ResolveCommonThreadPool.class).pool;
    }
}
