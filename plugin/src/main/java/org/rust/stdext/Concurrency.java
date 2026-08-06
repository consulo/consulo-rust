/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.stdext;

import consulo.logging.Logger;
import consulo.component.ProcessCanceledException;
import consulo.application.progress.ProgressManager;
import consulo.application.internal.ProgressIndicatorUtils;
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

    public static <T> T withLockAndCheckingCancelled(@Nonnull Lock lock, @Nonnull Supplier<T> action) {
        return ProgressIndicatorUtils.computeWithLockAndCheckingCanceled(lock, 10, TimeUnit.MILLISECONDS, action::get);
    }

    public static void awaitWithCheckCancelled(@Nonnull Condition condition) {
        // Consulo's ProgressIndicatorUtils.awaitWithCheckCanceled doesn't accept a Condition;
        // poll with a ThrowableComputable<Boolean> that returns true when the wait finished.
        ProgressIndicatorUtils.awaitWithCheckCanceled(
            (consulo.application.util.function.ThrowableComputable<Boolean, Exception>) () -> {
                condition.await(10, TimeUnit.MILLISECONDS);
                return Boolean.TRUE;
            });
    }
}
