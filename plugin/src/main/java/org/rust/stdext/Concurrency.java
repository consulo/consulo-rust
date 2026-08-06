/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.stdext;

import consulo.logging.Logger;
import consulo.component.ProcessCanceledException;
import consulo.application.progress.ProgressManager;
import jakarta.annotation.Nonnull;

import java.util.Queue;
import java.util.concurrent.*;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.function.Function;
import java.util.function.Supplier;

public final class Concurrency {
    private Concurrency() {
    }

    public static <T> T getWithCheckCanceled(@Nonnull Future<T> future, long timeoutMillis)
        throws TimeoutException, ExecutionException, InterruptedException {
        long deadline = System.nanoTime() + TimeUnit.MILLISECONDS.toNanos(timeoutMillis);
        while (true) {
            try {
                return future.get(10, TimeUnit.MILLISECONDS);
            } catch (TimeoutException e) {
                ProgressManager.checkCanceled();
                if (System.nanoTime() >= deadline) {
                    throw e;
                }
            }
        }
    }

    /**
     * Acquires {@code lock}, polling so that progress cancellation is still observed while waiting.
     * {@code ProgressIndicatorUtils.computeWithLockAndCheckingCanceled} is platform-internal, so the
     * poll loop is spelled out here.
     */
    public static <T> T withLockAndCheckingCancelled(@Nonnull Lock lock, @Nonnull Supplier<T> action) {
        while (true) {
            ProgressManager.checkCanceled();
            boolean acquired;
            try {
                acquired = lock.tryLock(10, TimeUnit.MILLISECONDS);
            }
            catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new ProcessCanceledException(e);
            }
            if (acquired) {
                try {
                    return action.get();
                }
                finally {
                    lock.unlock();
                }
            }
        }
    }

    /** Awaits {@code condition}, checking progress cancellation between short waits. */
    public static void awaitWithCheckCancelled(@Nonnull Condition condition) {
        while (true) {
            ProgressManager.checkCanceled();
            try {
                if (condition.await(10, TimeUnit.MILLISECONDS)) return;
            }
            catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new ProcessCanceledException(e);
            }
        }
    }
}
