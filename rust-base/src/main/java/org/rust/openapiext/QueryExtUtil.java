/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.openapiext;

import consulo.util.lang.function.Condition;
import jakarta.annotation.Nonnull;
import consulo.application.util.query.Query;

/**
 * Bridge class delegating to {@link QueryExt}.
 */
public final class QueryExtUtil {
    private QueryExtUtil() {
    }

    @Nonnull
    public static <U, V> consulo.application.util.query.Query<V> mapQuery(@Nonnull consulo.application.util.query.Query<U> query,
                                                                @Nonnull com.intellij.util.Function<U, V> f) {
        return QueryExt.mapQuery(query, f);
    }

    @Nonnull
    public static <U> consulo.application.util.query.Query<U> filterQuery(@Nonnull consulo.application.util.query.Query<U> query,
                                                               @Nonnull Condition<U> condition) {
        return QueryExt.filterQuery(query, condition);
    }
}
