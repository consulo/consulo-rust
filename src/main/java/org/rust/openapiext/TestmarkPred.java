/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.openapiext;

import org.jetbrains.annotations.TestOnly;

import java.util.function.Supplier;

public interface TestmarkPred {
    @TestOnly
    <T> T checkHit(Supplier<T> f);

    @TestOnly
    <T> T checkNotHit(Supplier<T> f);
}
