/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.lang.core.resolve2.util;

import gnu.trove.THash;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/** Optimized version of {@code THashMap<K, SmartList<V>>} */
@SuppressWarnings("unchecked")
public class SmartListMap<K, V> extends THashMapBase<K, List<V>> {

    private Object[] items = THash.EMPTY_OBJECT_ARRAY;

    @Override
    public int size() {
        return _size;
    }

    @Nullable
    @Override
    protected List<V> getValueAtIndex(int index) {
        Object value = items[index];
        if (value == null) return null;
        if (value instanceof List<?>) {
            return (List<V>) value;
        } else {
            return Collections.singletonList((V) value);
        }
    }

    @Override
    protected void setValueAtIndex(int index, @NotNull List<V> value) {
        items[index] = value.size() == 1 ? value.get(0) : value;
    }

    public void addValue(@NotNull K key, @NotNull V value) {
        int index = insertionIndex(key);
        boolean alreadyStored = index < 0;
        if (alreadyStored) {
            int indexAdjusted = -index - 1;
            Object existing = items[indexAdjusted];
            if (existing instanceof List<?>) {
                ((List<V>) existing).add(value);
            } else {
                ArrayList<Object> list = new ArrayList<>();
                list.add(existing);
                list.add(value);
                items[indexAdjusted] = list;
            }
        } else {
            _set[index] = key;
            items[index] = value;
            postInsertHook();
        }
    }

    @Override
    protected void createNewArrays(int capacity) {
        items = new Object[capacity];
    }

    @Override
    protected void rehash(int newCapacity) {
        Object[] oldItems = items;
        rehashTemplate(newCapacity, (newIndex, oldIndex) -> {
            items[newIndex] = oldItems[oldIndex];
        });
    }
}
