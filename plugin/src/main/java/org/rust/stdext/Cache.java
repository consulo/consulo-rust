/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.stdext;

import jakarta.annotation.Nonnull;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Supplier;

public interface Cache<K, V> {
    V getOrPut(@Nonnull K key, @Nonnull Supplier<V> defaultValue);

    @Nonnull
    static <K, V> Cache<K, V> create() {
        Map<K, V> map = new HashMap<>();
        return (key, defaultValue) -> map.computeIfAbsent(key, k -> defaultValue.get());
    }
}
