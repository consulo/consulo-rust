/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.stdext;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.function.Supplier;

public final class Lazy<T> {
    @NotNull
    private final Supplier<T> myInitializer;
    @Nullable
    private volatile T myValue;
    private volatile boolean myInitialized;

    public Lazy(@NotNull Supplier<T> initializer) {
        myInitializer = initializer;
    }

    @Nullable
    public T getValue() {
        if (!myInitialized) {
            synchronized (this) {
                if (!myInitialized) {
                    myValue = myInitializer.get();
                    myInitialized = true;
                }
            }
        }
        return myValue;
    }
}
