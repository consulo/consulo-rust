/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.stdext;

import com.intellij.util.SmartList;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;
import java.util.function.BiConsumer;
import java.util.function.Function;
import java.util.function.Predicate;

public final class CollectionsUtil {
    private static final int INT_MAX_POWER_OF_TWO = Integer.MAX_VALUE / 2 + 1;

    private CollectionsUtil() {
    }

    @NotNull
    public static <T> List<T> singleOrFilter(@NotNull List<T> list, @NotNull Predicate<T> predicate) {
        if (list.size() < 2) return list;
        List<T> result = new ArrayList<>();
        for (T item : list) {
            if (predicate.test(item)) {
                result.add(item);
            }
        }
        return result;
    }

    @NotNull
    public static <T> List<T> singleOrLet(@NotNull List<T> list, @NotNull Function<List<T>, List<T>> function) {
        if (list.size() < 2) return list;
        return function.apply(list);
    }

    @NotNull
    public static <T> List<T> notEmptyOrLet(@NotNull List<T> list, @NotNull Function<List<T>, List<T>> function) {
        if (!list.isEmpty()) return list;
        return function.apply(list);
    }

    @NotNull
    public static <T> List<T> optimizeList(@NotNull SmartList<T> list) {
        switch (list.size()) {
            case 0:
                return Collections.emptyList();
            case 1:
                return Collections.singletonList(list.get(0));
            default:
                list.trimToSize();
                return list;
        }
    }

    public static int mapCapacity(int expectedSize) {
        if (expectedSize < 3) {
            return expectedSize + 1;
        }
        if (expectedSize < INT_MAX_POWER_OF_TWO) {
            return expectedSize + expectedSize / 3;
        }
        return Integer.MAX_VALUE;
    }

    @NotNull
    public static <K, V> HashMap<K, V> newHashMapWithExpectedSize(int size) {
        return new HashMap<>(mapCapacity(size));
    }

    public static int collectionSizeOrDefault(@NotNull Iterable<?> iterable, int defaultSize) {
        if (iterable instanceof Collection<?>) {
            return ((Collection<?>) iterable).size();
        }
        return defaultSize;
    }

    public static int makeBitMask(int bitToSet) {
        return 1 << bitToSet;
    }

    @NotNull
    public static <K, V1, V2> List<com.intellij.openapi.util.Pair<V1, V2>> zipValues(@NotNull Map<K, V1> map1, @NotNull Map<K, V2> map2) {
        List<com.intellij.openapi.util.Pair<V1, V2>> result = new ArrayList<>();
        for (Map.Entry<K, V1> entry : map1.entrySet()) {
            V2 v2 = map2.get(entry.getKey());
            if (v2 != null) {
                result.add(new com.intellij.openapi.util.Pair<>(entry.getValue(), v2));
            }
        }
        return result;
    }

    public static <T> boolean intersects(@NotNull Collection<T> set, @NotNull Iterable<T> other) {
        for (T item : other) {
            if (set.contains(item)) return true;
        }
        return false;
    }

    public static <T> void joinToWithBuffer(
        @NotNull Iterable<T> iterable,
        @NotNull StringBuilder buffer,
        @NotNull CharSequence separator,
        @NotNull CharSequence prefix,
        @NotNull CharSequence postfix,
        @NotNull BiConsumer<T, StringBuilder> action
    ) {
        buffer.append(prefix);
        boolean needInsertSeparator = false;
        for (T element : iterable) {
            if (needInsertSeparator) {
                buffer.append(separator);
            }
            action.accept(element, buffer);
            needInsertSeparator = true;
        }
        buffer.append(postfix);
    }

    @Nullable
    public static <T> T nextOrNull(@NotNull Iterator<T> iterator) {
        return iterator.hasNext() ? iterator.next() : null;
    }

    @NotNull
    public static <T> T removeLast(@NotNull List<T> list) {
        return list.remove(list.size() - 1);
    }

    @NotNull
    @SafeVarargs
    public static <T> ArrayDeque<T> dequeOf(@NotNull T... elements) {
        ArrayDeque<T> deque = new ArrayDeque<>();
        Collections.addAll(deque, elements);
        return deque;
    }

    public static <T> boolean isSortedWith(@NotNull List<T> list, @NotNull Comparator<T> comparator) {
        for (int i = 0; i < list.size() - 1; i++) {
            if (comparator.compare(list.get(i), list.get(i + 1)) > 0) {
                return false;
            }
        }
        return true;
    }

    @NotNull
    public static <K, V> Map<K, V> replaceTrivialMap(@NotNull Map<K, V> map) {
        switch (map.size()) {
            case 0:
                return Collections.emptyMap();
            case 1: {
                Map.Entry<K, V> entry = map.entrySet().iterator().next();
                return Collections.singletonMap(entry.getKey(), entry.getValue());
            }
            default:
                return map;
        }
    }

    public static <T> void swapRemoveAt(@NotNull List<T> list, int index) {
        if (index == list.size() - 1) {
            list.remove(list.size() - 1);
        } else {
            list.set(index, list.remove(list.size() - 1));
        }
    }
}
