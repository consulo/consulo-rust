/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.stdext;

import org.jetbrains.annotations.NotNull;

import java.util.Collection;
import java.util.List;
import java.util.function.Predicate;

/**
 * Bridge class delegating to {@link CollectionsKt}.
 */
public final class CollectionExtUtil {
    private CollectionExtUtil() {
    }

    @NotNull
    public static <T> List<T> singleOrFilter(@NotNull List<T> list, @NotNull Predicate<T> predicate) {
        return CollectionsUtil.singleOrFilter(list, predicate);
    }

    public static <T> boolean intersects(@NotNull Collection<T> a, @NotNull Collection<T> b) {
        for (T item : a) {
            if (b.contains(item)) return true;
        }
        return false;
    }
}
