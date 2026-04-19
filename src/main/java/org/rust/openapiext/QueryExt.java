/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.openapiext;

import com.intellij.openapi.util.Condition;
import com.intellij.util.FilteredQuery;
import com.intellij.util.InstanceofQuery;
import com.intellij.util.Query;
import org.jetbrains.annotations.NotNull;

public final class QueryExt {
    private QueryExt() {
    }

    @NotNull
    public static <U> Query<U> filterQuery(@NotNull Query<U> query, @NotNull Condition<U> condition) {
        return new FilteredQuery<>(query, condition);
    }

    @NotNull
    public static <V> Query<V> filterIsInstanceQuery(@NotNull Query<?> query, @NotNull Class<V> clazz) {
        return new InstanceofQuery<>(query, clazz);
    }

    @NotNull
    public static <U, V> Query<V> mapQuery(@NotNull Query<U> query, @NotNull com.intellij.util.Function<U, V> f) {
        return query.mapping(f::fun);
    }
}
