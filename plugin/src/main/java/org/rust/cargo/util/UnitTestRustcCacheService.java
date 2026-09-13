/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.cargo.util;

import consulo.application.Application;
import org.rust.cargo.api.toolchain.RustcVersion;

import java.util.function.BooleanSupplier;
import java.util.function.Supplier;
import consulo.annotation.component.ServiceAPI;
import consulo.annotation.component.ComponentScope;

/**
 * This cache is used *only* in unit tests.
 *
 * We assume that {@code sysroot}, a {@code list of targets}, {@code --print cfg} and {@code stdlib} are not changed
 * (in unit tests) until the version of rustc is changed, so we can cache these values for all tests.
 * The cache significantly speeds up heavy tests with a full toolchain ({@code RsWithToolchainTestBase})
 */
@ServiceAPI(ComponentScope.APPLICATION)
public interface UnitTestRustcCacheService {

    <T> T cachedInner(
        RustcVersion rustcVersion,
        BooleanSupplier cacheIf,
        Supplier<T> computation
    );

    static <T> T cached(RustcVersion rustcVersion, BooleanSupplier cacheIf, Supplier<T> computation) {
        return Application.get().getInstance(UnitTestRustcCacheService.class).cachedInner(rustcVersion, cacheIf, computation);
    }

    static <T> T cached(RustcVersion rustcVersion, Supplier<T> computation) {
        return cached(rustcVersion, () -> true, computation);
    }
}
