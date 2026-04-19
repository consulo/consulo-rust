/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.openapiext;

import com.intellij.openapi.util.Condition;
import com.intellij.util.FilteredQuery;
import com.intellij.util.InstanceofQuery;
import org.jetbrains.annotations.NotNull;

// Be careful with queries: they are Iterables, so they have
// map, filter and friends, which convert them to List.

public final class Query {
    private Query() {
    }

    @NotNull
    public static <U> com.intellij.util.Query<U> filterQuery(@NotNull com.intellij.util.Query<U> query,
                                                              @NotNull Condition<U> condition) {
        return new FilteredQuery<>(query, condition);
    }

    @NotNull
    public static <V> com.intellij.util.Query<V> filterIsInstanceQuery(@NotNull com.intellij.util.Query<?> query,
                                                                        @NotNull Class<V> clazz) {
        return new InstanceofQuery<>(query, clazz);
    }

    @NotNull
    public static <U, V> com.intellij.util.Query<V> mapQuery(@NotNull com.intellij.util.Query<U> query,
                                                              @NotNull com.intellij.util.Function<U, V> f) {
        return query.mapping(f::fun);
    }
}
