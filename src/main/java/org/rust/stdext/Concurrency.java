/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.stdext;

import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.progress.ProcessCanceledException;
import com.intellij.openapi.progress.ProgressManager;
import com.intellij.openapi.progress.util.ProgressIndicatorUtils;
import org.jetbrains.annotations.NotNull;

import java.util.Queue;
import java.util.concurrent.*;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.function.Function;
import java.util.function.Supplier;

public final class Concurrency {
    private Concurrency() {
    }

    public static <T> T getWithCheckCanceled(@NotNull Future<T> future, long timeoutMillis)
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

    public static <T> T withLockAndCheckingCancelled(@NotNull Lock lock, @NotNull Supplier<T> action) {
        return ProgressIndicatorUtils.computeWithLockAndCheckingCanceled(lock, 10, TimeUnit.MILLISECONDS, action::get);
    }

    public static void awaitWithCheckCancelled(@NotNull Condition condition) {
        ProgressIndicatorUtils.awaitWithCheckCanceled(condition);
    }
}
