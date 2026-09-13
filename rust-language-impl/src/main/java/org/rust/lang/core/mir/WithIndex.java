/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.lang.core.mir;

import jakarta.annotation.Nonnull;

import java.util.List;

public interface WithIndex {
    int getIndex();

    /**
     * Allocates a new element in the list using the constructor, passing the next index.
     */
    static <T extends WithIndex> T allocate(@Nonnull List<T> list, @Nonnull java.util.function.IntFunction<T> constructor) {
        int index = list.size();
        T value = constructor.apply(index);
        list.add(value);
        return value;
    }

    /**
     * Gets an element from a list using a WithIndex key.
     */
    @Nonnull
    static <T> T get(@Nonnull List<T> list, @Nonnull WithIndex key) {
        return list.get(key.getIndex());
    }
}
