/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.openapiext;

import java.util.function.Supplier;

public final class EmptyTestmark implements TestmarkPred {
    public static final EmptyTestmark INSTANCE = new EmptyTestmark();

    private EmptyTestmark() {
    }

    @Override
    public <T> T checkHit(Supplier<T> f) {
        return f.get();
    }

    @Override
    public <T> T checkNotHit(Supplier<T> f) {
        return f.get();
    }
}
