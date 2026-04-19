/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.cargo.util;

import com.intellij.openapi.util.Pair;
import org.jetbrains.annotations.TestOnly;
import org.rust.cargo.toolchain.impl.RustcVersion;

import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.BooleanSupplier;
import java.util.function.Supplier;

@TestOnly
public class TestUnitTestRustcCacheService implements UnitTestRustcCacheService {
    private final ConcurrentHashMap<Pair<RustcVersion, Class<?>>, Optional<Object>> cache = new ConcurrentHashMap<>();

    @Override
    @SuppressWarnings("unchecked")
    public <T> T cachedInner(
        RustcVersion rustcVersion,
        BooleanSupplier cacheIf,
        Supplier<T> computation
    ) {
        if (rustcVersion == null || !cacheIf.getAsBoolean()) return computation.get();
        Class<?> cls = computation.getClass();
        Pair<RustcVersion, Class<?>> key = Pair.create(rustcVersion, cls);
        Optional<Object> result = cache.computeIfAbsent(key, k -> Optional.ofNullable(computation.get()));
        return (T) result.orElse(null);
    }
}
