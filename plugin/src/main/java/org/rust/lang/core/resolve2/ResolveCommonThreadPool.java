/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.lang.core.resolve2;

import consulo.disposer.Disposable;
import com.intellij.openapi.components.Service;
import jakarta.annotation.Nonnull;
import org.rust.openapiext.OpenApiUtil;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.ForkJoinPool.ForkJoinWorkerThreadFactory;

@Service
public final class ResolveCommonThreadPool implements Disposable {

    private static final String THREAD_NAME_PREFIX = "Rust-resolve-thread-";

    @Nonnull
    private final ExecutorService pool;

    public ResolveCommonThreadPool() {
        this.pool = createPool();
    }

    @Nonnull
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

    @Nonnull
    public static ExecutorService get() {
        return consulo.application.ApplicationManager.getApplication()
            .getService(ResolveCommonThreadPool.class).pool;
    }
}
