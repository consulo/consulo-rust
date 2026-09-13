/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.lang.core.mir.util;

import jakarta.annotation.Nonnull;
import org.rust.lang.core.mir.WithIndex;

import java.util.function.IntFunction;

public class IndexAlloc<T extends WithIndex> {
    private int counter = 0;

    public int getSize() {
        return counter;
    }

    @Nonnull
    public T allocate(@Nonnull IntFunction<T> constructor) {
        return constructor.apply(counter++);
    }
}
