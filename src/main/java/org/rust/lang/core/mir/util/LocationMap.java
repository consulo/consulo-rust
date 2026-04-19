/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.lang.core.mir.util;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.rust.lang.core.mir.schemas.MirBody;
import org.rust.lang.core.mir.schemas.MirLocation;

import java.util.*;
import java.util.function.Supplier;

public class LocationMap<V> implements Map<MirLocation, V> {
    @NotNull
    private final Object[][] inner;

    public LocationMap(@NotNull MirBody body) {
        List<?> basicBlocks = body.getBasicBlocks();
        inner = new Object[basicBlocks.size()][];
        for (int i = 0; i < basicBlocks.size(); i++) {
            Object block = basicBlocks.get(i);
            // Each block has statements.size() + 1 slots (for the terminator)
            int statementsSize = getStatementsSize(block);
            inner[i] = new Object[statementsSize + 1];
        }
    }

    @SuppressWarnings("unchecked")
    private int getStatementsSize(@NotNull Object block) {
        // Use reflection-safe approach: MirBasicBlock has getStatements()
        try {
            java.lang.reflect.Method m = block.getClass().getMethod("getStatements");
            List<?> statements = (List<?>) m.invoke(block);
            return statements.size();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @NotNull
    public V getOrPut(@NotNull MirLocation key, @NotNull Supplier<V> defaultValue) {
        V value = get(key);
        if (value == null) {
            value = defaultValue.get();
            put(key, value);
        }
        return value;
    }

    @Override
    @NotNull
    public Set<Entry<MirLocation, V>> entrySet() {
        throw new UnsupportedOperationException();
    }

    @Override
    @NotNull
    public Set<MirLocation> keySet() {
        throw new UnsupportedOperationException();
    }

    @Override
    public int size() {
        throw new UnsupportedOperationException();
    }

    @Override
    @NotNull
    public Collection<V> values() {
        throw new UnsupportedOperationException();
    }

    @Override
    public void clear() {
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean isEmpty() {
        throw new UnsupportedOperationException();
    }

    @Override
    @Nullable
    public V remove(Object key) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void putAll(@NotNull Map<? extends MirLocation, ? extends V> from) {
        throw new UnsupportedOperationException();
    }

    @SuppressWarnings("unchecked")
    @Override
    @Nullable
    public V put(@NotNull MirLocation key, @Nullable V value) {
        Object[] perStmt = inner[key.getBlock().getIndex()];
        V oldValue = (V) perStmt[key.getStatementIndex()];
        perStmt[key.getStatementIndex()] = value;
        return oldValue;
    }

    @SuppressWarnings("unchecked")
    @Override
    @Nullable
    public V get(Object key) {
        MirLocation loc = (MirLocation) key;
        return (V) inner[loc.getBlock().getIndex()][loc.getStatementIndex()];
    }

    @Override
    public boolean containsValue(Object value) {
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean containsKey(Object key) {
        return get(key) != null;
    }
}
