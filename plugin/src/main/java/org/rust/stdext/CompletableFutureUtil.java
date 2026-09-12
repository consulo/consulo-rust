/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.stdext;

import jakarta.annotation.Nonnull;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executor;
import java.util.concurrent.Future;
import java.util.function.Supplier;

public final class CompletableFutureUtil {
    private CompletableFutureUtil() {
    }

    @Nonnull
    public static <T> CompletableFuture<T> supplyAsync(@Nonnull Executor executor, @Nonnull Supplier<T> supplier) {
        return CompletableFuture.supplyAsync(supplier, executor);
    }

    public static <T> T getWithRethrow(@Nonnull Future<T> future) throws Exception {
        try {
            return future.get();
        } catch (ExecutionException e) {
            Throwable cause = e.getCause();
            if (cause instanceof RuntimeException runtime) {
                throw runtime;
            }
            if (cause instanceof Error error) {
                throw error;
            }
            if (cause instanceof Exception checked) {
                throw checked;
            }
            throw e;
        }
    }
}
