/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.stdext;

import org.rust.stdext.Lazy;
import org.jetbrains.annotations.NotNull;

import java.util.concurrent.locks.ReentrantLock;
import java.util.function.Supplier;

public final class SynchronizedCancellableLazy<T> implements Supplier<T> {
    private static final Object UNINITIALIZED = new Object();

    private Supplier<T> myInitializer;
    @SuppressWarnings("unchecked")
    private volatile Object myValue = UNINITIALIZED;
    private final ReentrantLock myLock = new ReentrantLock();

    public SynchronizedCancellableLazy(@NotNull Supplier<T> initializer) {
        this.myInitializer = initializer;
    }

    @SuppressWarnings("unchecked")
    @Override
    @NotNull
    public T get() {
        Object v1 = myValue;
        if (v1 != UNINITIALIZED) {
            return (T) v1;
        }

        return Concurrency.withLockAndCheckingCancelled(myLock, () -> {
            Object v2 = myValue;
            if (v2 != UNINITIALIZED) {
                return (T) v2;
            }
            T typedValue = myInitializer.get();
            myValue = typedValue;
            myInitializer = null;
            return typedValue;
        });
    }

    public boolean isInitialized() {
        return myValue != UNINITIALIZED;
    }

    @Override
    @NotNull
    public String toString() {
        return isInitialized() ? String.valueOf(get()) : "Lazy value not initialized yet.";
    }

    @NotNull
    public static <T> SynchronizedCancellableLazy<T> cancelableLazy(@NotNull Supplier<T> initializer) {
        return new SynchronizedCancellableLazy<>(initializer);
    }
}
