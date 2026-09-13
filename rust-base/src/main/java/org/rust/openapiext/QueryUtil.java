/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.openapiext;

import consulo.application.util.query.InstanceofQuery;
import consulo.application.util.query.Query;
import jakarta.annotation.Nonnull;

import java.util.Collection;
import java.util.function.Function;

public final class QueryUtil {
    private QueryUtil() {}

    @Nonnull
    public static <T, R> Query<R> mapQuery(@Nonnull Query<T> query, @Nonnull Function<T, R> mapper) {
        return query.mapping(mapper::apply);
    }

    @Nonnull
    public static <T, R> Query<R> filterIsInstanceQuery(@Nonnull Query<T> query, @Nonnull Class<R> clazz) {
        return new InstanceofQuery<>(query, clazz);
    }

    @Nonnull
    public static <T> Collection<T> getElements(@Nonnull Query<T> query) {
        return query.findAll();
    }
}
