/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.openapiext;

import com.intellij.util.InstanceofQuery;
import com.intellij.util.Query;
import org.jetbrains.annotations.NotNull;

import java.util.Collection;
import java.util.function.Function;

public final class QueryUtil {
    private QueryUtil() {}

    @NotNull
    public static <T, R> Query<R> mapQuery(@NotNull Query<T> query, @NotNull Function<T, R> mapper) {
        return query.mapping(mapper::apply);
    }

    @NotNull
    public static <T, R> Query<R> filterIsInstanceQuery(@NotNull Query<T> query, @NotNull Class<R> clazz) {
        return new InstanceofQuery<>(query, clazz);
    }

    @NotNull
    public static <T> Collection<T> getElements(@NotNull Query<T> query) {
        return query.findAll();
    }
}
