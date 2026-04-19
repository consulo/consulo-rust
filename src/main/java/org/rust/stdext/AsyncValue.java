/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.stdext;

import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.progress.ProcessCanceledException;
import org.jetbrains.annotations.NotNull;

import java.util.Queue;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.function.Function;

public class AsyncValue<T> {
    private static final Logger LOG = Logger.getInstance(AsyncValue.class);

    private volatile T myCurrent;
    private final Queue<Function<T, CompletableFuture<Void>>> myUpdates = new ConcurrentLinkedQueue<>();
    private boolean myRunning = false;

    public AsyncValue(@NotNull T initial) {
        this.myCurrent = initial;
    }

    @NotNull
    public T getCurrentState() {
        return myCurrent;
    }

    @NotNull
    public CompletableFuture<T> updateAsync(@NotNull Function<T, CompletableFuture<T>> updater) {
        CompletableFuture<T> result = new CompletableFuture<>();
        myUpdates.add(current -> updater.apply(current).handle((next, err) -> {
            if (err == null) {
                myCurrent = next;
                result.complete(next);
            } else {
                if (!(err instanceof ProcessCanceledException
                    || (err instanceof CompletionException && err.getCause() instanceof ProcessCanceledException))) {
                    LOG.error(err);
                }
                result.completeExceptionally(err);
            }
            return null;
        }));
        startUpdateProcessing();
        return result;
    }

    @NotNull
    public CompletableFuture<T> updateSync(@NotNull Function<T, T> updater) {
        return updateAsync(current -> CompletableFuture.completedFuture(updater.apply(current)));
    }

    private synchronized void startUpdateProcessing() {
        if (myRunning || myUpdates.isEmpty()) return;
        Function<T, CompletableFuture<Void>> nextUpdate = myUpdates.remove();
        myRunning = true;
        nextUpdate.apply(myCurrent).whenComplete((v, err) -> {
            stopUpdateProcessing();
            startUpdateProcessing();
        });
    }

    private synchronized void stopUpdateProcessing() {
        if (!myRunning) throw new IllegalStateException("Not running");
        myRunning = false;
    }
}
