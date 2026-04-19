/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.stdext;

import org.jetbrains.annotations.NotNull;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executor;
import java.util.concurrent.Future;
import java.util.function.Supplier;

public final class CompletableFutureUtil {
    private CompletableFutureUtil() {
    }

    @NotNull
    public static <T> CompletableFuture<T> supplyAsync(@NotNull Executor executor, @NotNull Supplier<T> supplier) {
        return CompletableFuture.supplyAsync(supplier, executor);
    }

    public static <T> T getWithRethrow(@NotNull Future<T> future) throws Exception {
        try {
            return future.get();
        } catch (ExecutionException e) {
            Throwable cause = e.getCause();
            if (cause instanceof Exception) {
                throw (Exception) cause;
            }
            throw e;
        }
    }
}
