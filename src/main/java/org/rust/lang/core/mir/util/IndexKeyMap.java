/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.lang.core.mir.util;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.rust.lang.core.mir.WithIndex;

import java.util.*;
import java.util.function.Supplier;

public class IndexKeyMap<K extends WithIndex, V> implements Map<K, V> {
    @NotNull
    private final List<V> inner;

    public IndexKeyMap() {
        this(new ArrayList<>());
    }

    private IndexKeyMap(@NotNull List<V> inner) {
        this.inner = inner;
    }

    @Override
    @NotNull
    public Set<Entry<K, V>> entrySet() {
        throw new UnsupportedOperationException();
    }

    @Override
    @NotNull
    public Set<K> keySet() {
        throw new UnsupportedOperationException();
    }

    @Override
    public int size() {
        int count = 0;
        for (V v : inner) {
            if (v != null) count++;
        }
        return count;
    }

    @Override
    @NotNull
    public Collection<V> values() {
        throw new UnsupportedOperationException();
    }

    @Override
    public void clear() {
        for (int i = 0; i < inner.size(); i++) {
            inner.set(i, null);
        }
    }

    @Override
    public boolean isEmpty() {
        return size() == 0;
    }

    @Override
    @Nullable
    public V remove(Object key) {
        @SuppressWarnings("unchecked")
        K k = (K) key;
        V oldValue = inner.get(k.getIndex());
        inner.set(k.getIndex(), null);
        return oldValue;
    }

    @Override
    public void putAll(@NotNull Map<? extends K, ? extends V> from) {
        for (Entry<? extends K, ? extends V> entry : from.entrySet()) {
            put(entry.getKey(), entry.getValue());
        }
    }

    @Override
    @Nullable
    public V put(@NotNull K key, @Nullable V value) {
        int index = key.getIndex();
        while (inner.size() <= index) {
            inner.add(null);
        }
        V oldValue = inner.get(index);
        inner.set(index, value);
        return oldValue;
    }

    @Override
    @Nullable
    public V get(Object key) {
        @SuppressWarnings("unchecked")
        K k = (K) key;
        if (k.getIndex() >= inner.size()) return null;
        return inner.get(k.getIndex());
    }

    @NotNull
    public V getOrPut(@NotNull K key, @NotNull Supplier<V> defaultValue) {
        V value = get(key);
        if (value == null) {
            value = defaultValue.get();
            put(key, value);
        }
        return value;
    }

    @Override
    public boolean containsValue(Object value) {
        return inner.contains(value);
    }

    @Override
    public boolean containsKey(Object key) {
        return get(key) != null;
    }

    @SuppressWarnings("unchecked")
    @NotNull
    public static <K extends WithIndex, V> Map<K, V> fromListUnchecked(@NotNull List<V> list) {
        return new IndexKeyMap<>(new ArrayList<>(list));
    }
}
