/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.stdext;

import jakarta.annotation.Nonnull;

import java.util.Collection;
import java.util.List;
import java.util.function.Predicate;

/**
 * Bridge class delegating to {@link CollectionsKt}.
 */
public final class CollectionExtUtil {
    private CollectionExtUtil() {
    }

    @Nonnull
    public static <T> List<T> singleOrFilter(@Nonnull List<T> list, @Nonnull Predicate<T> predicate) {
        return CollectionsUtil.singleOrFilter(list, predicate);
    }

    public static <T> boolean intersects(@Nonnull Collection<T> a, @Nonnull Collection<T> b) {
        for (T item : a) {
            if (b.contains(item)) return true;
        }
        return false;
    }
}
