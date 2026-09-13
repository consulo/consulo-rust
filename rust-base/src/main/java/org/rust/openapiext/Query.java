/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.openapiext;
import java.util.function.Function;

import consulo.util.lang.function.Condition;
import consulo.application.util.query.FilteredQuery;
import consulo.application.util.query.InstanceofQuery;
import jakarta.annotation.Nonnull;

// Be careful with queries: they are Iterables, so they have
// map, filter and friends, which convert them to List.

public final class Query {
    private Query() {
    }

    @Nonnull
    public static <U> consulo.application.util.query.Query<U> filterQuery(@Nonnull consulo.application.util.query.Query<U> query,
                                                              @Nonnull Condition<U> condition) {
        return new FilteredQuery<>(query, condition);
    }

    @Nonnull
    public static <V> consulo.application.util.query.Query<V> filterIsInstanceQuery(@Nonnull consulo.application.util.query.Query<?> query,
                                                                        @Nonnull Class<V> clazz) {
        return new InstanceofQuery<>(query, clazz);
    }

    @Nonnull
    public static <U, V> consulo.application.util.query.Query<V> mapQuery(@Nonnull consulo.application.util.query.Query<U> query,
                                                              @Nonnull com.intellij.util.Function<U, V> f) {
        return query.mapping(f::fun);
    }
}
