/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.openapiext;

import com.intellij.openapi.util.Condition;
import org.jetbrains.annotations.NotNull;

/**
 * Bridge class delegating to {@link QueryExt}.
 */
public final class QueryExtUtil {
    private QueryExtUtil() {
    }

    @NotNull
    public static <U, V> com.intellij.util.Query<V> mapQuery(@NotNull com.intellij.util.Query<U> query,
                                                                @NotNull com.intellij.util.Function<U, V> f) {
        return QueryExt.mapQuery(query, f);
    }

    @NotNull
    public static <U> com.intellij.util.Query<U> filterQuery(@NotNull com.intellij.util.Query<U> query,
                                                               @NotNull Condition<U> condition) {
        return QueryExt.filterQuery(query, condition);
    }
}
