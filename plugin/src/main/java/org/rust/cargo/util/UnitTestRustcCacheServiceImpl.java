/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.cargo.util;

import org.rust.cargo.toolchain.impl.RustcVersion;

import java.util.function.BooleanSupplier;
import java.util.function.Supplier;

public class UnitTestRustcCacheServiceImpl implements UnitTestRustcCacheService {
    @Override
    public <T> T cachedInner(
        RustcVersion rustcVersion,
        BooleanSupplier cacheIf,
        Supplier<T> computation
    ) {
        return computation.get();
    }
}
