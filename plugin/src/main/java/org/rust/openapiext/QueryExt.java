/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.openapiext;
import java.util.function.Function;

import consulo.util.lang.function.Condition;
import consulo.application.util.query.FilteredQuery;
import consulo.application.util.query.InstanceofQuery;
import consulo.application.util.query.Query;
import jakarta.annotation.Nonnull;

public final class QueryExt {
    private QueryExt() {
    }

    @Nonnull
    public static <U> Query<U> filterQuery(@Nonnull Query<U> query, @Nonnull Condition<U> condition) {
        return new FilteredQuery<>(query, condition);
    }

    @Nonnull
    public static <V> Query<V> filterIsInstanceQuery(@Nonnull Query<?> query, @Nonnull Class<V> clazz) {
        return new InstanceofQuery<>(query, clazz);
    }

    @Nonnull
    public static <U, V> Query<V> mapQuery(@Nonnull Query<U> query, @Nonnull com.intellij.util.Function<U, V> f) {
        return query.mapping(f::fun);
    }
}
