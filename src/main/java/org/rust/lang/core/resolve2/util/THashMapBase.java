/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.lang.core.resolve2.util;

import gnu.trove.THash;
import gnu.trove.TObjectHash;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;
import java.util.function.BiConsumer;

/**
 * Copy of THashMap, abstracted over values array.
 * Deletion is not supported (we don't need it).
 */
@SuppressWarnings("unchecked")
public abstract class THashMapBase<K, V> extends TObjectHash<K> implements Map<K, V> {

    @Nullable
    protected abstract V getValueAtIndex(int index);

    protected abstract void setValueAtIndex(int index, @NotNull V value);

    @Override
    public int size() {
        return _size;
    }

    @Nullable
    @Override
    public V put(K key, V value) {
        V previous = null;
        int index = insertionIndex(key);
        boolean alreadyStored = index < 0;
        if (alreadyStored) {
            index = -index - 1;
            previous = getValueAtIndex(index);
        }
        _set[index] = key;
        setValueAtIndex(index, value);
        if (!alreadyStored) {
            postInsertHook();
        }
        return previous;
    }

    @Nullable
    @Override
    public V putIfAbsent(K key, V value) {
        int index = insertionIndex(key);
        boolean alreadyStored = index < 0;
        if (alreadyStored) return getValueAtIndex(-index - 1);

        _set[index] = key;
        setValueAtIndex(index, value);
        postInsertHook();
        return null;
    }

    /** Key deletion is not supported, that's why usedFreeSlot is always true */
    protected void postInsertHook() {
        postInsertHook(true);
    }

    protected abstract void createNewArrays(int capacity);

    @Override
    protected int setUp(int initialCapacity) {
        int capacity = super.setUp(initialCapacity);
        if (initialCapacity != THash.JUST_CREATED_CAPACITY) {
            createNewArrays(capacity);
        }
        return capacity;
    }

    @FunctionalInterface
    protected interface MoveValueAction {
        void move(int newIndex, int oldIndex);
    }

    protected void rehashTemplate(int newCapacity, @NotNull MoveValueAction moveValue) {
        int oldCapacity = _set.length;
        Object[] oldKeys = _set;
        _set = new Object[newCapacity];
        createNewArrays(newCapacity);
        int i = oldCapacity;
        while (i-- > 0) {
            if (oldKeys[i] != null) {
                K oldKey = (K) oldKeys[i];
                int index = insertionIndex(oldKey);
                if (index < 0) {
                    throwObjectContractViolation(_set[-index - 1], oldKey);
                }
                _set[index] = oldKey;
                moveValue.move(index, i);
            }
        }
    }

    @Nullable
    @Override
    @SuppressWarnings("unchecked")
    public V get(Object key) {
        int index = index((K) key);
        return index < 0 ? null : getValueAtIndex(index);
    }

    /** Unlike default implementation, doesn't call containsKey */
    @Override
    public V getOrDefault(Object key, V defaultValue) {
        V result = get(key);
        return result != null ? result : defaultValue;
    }

    @Override
    public void clear() {
        throw new UnsupportedOperationException();
    }

    @Override
    public V remove(Object key) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void removeAt(int index) {
        throw new UnsupportedOperationException();
    }

    @NotNull
    @Override
    public Collection<V> values() {
        return new ValueView();
    }

    @NotNull
    @Override
    public Set<K> keySet() {
        return new KeyView();
    }

    @NotNull
    @Override
    public Set<Map.Entry<K, V>> entrySet() {
        return new EntryView();
    }

    @Override
    public boolean containsValue(Object value) {
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean containsKey(Object key) {
        return contains(key);
    }

    @Override
    public void putAll(@NotNull Map<? extends K, ? extends V> from) {
        ensureCapacity(from.size());
        for (Map.Entry<? extends K, ? extends V> entry : from.entrySet()) {
            put(entry.getKey(), entry.getValue());
        }
    }

    @Override
    public boolean isEmpty() {
        return _size == 0;
    }

    private class KeyView extends AbstractSet<K> {
        @NotNull
        @Override
        public Iterator<K> iterator() {
            return new THashIterator<K>() {
                @Override
                protected K objectAtIndex(int index) {
                    return (K) _set[index];
                }
            };
        }

        @Override
        public int size() {
            return _size;
        }

        @Override
        public boolean contains(Object element) {
            return THashMapBase.this.contains(element);
        }
    }

    private class ValueView extends AbstractCollection<V> {
        @NotNull
        @Override
        public Iterator<V> iterator() {
            return new THashIterator<V>() {
                @Override
                protected V objectAtIndex(int index) {
                    return getValueAtIndex(index);
                }
            };
        }

        @Override
        public int size() {
            return _size;
        }

        @Override
        public boolean contains(Object element) {
            throw new UnsupportedOperationException();
        }
    }

    private class EntryView extends AbstractSet<Map.Entry<K, V>> {
        @NotNull
        @Override
        public Iterator<Map.Entry<K, V>> iterator() {
            return new THashIterator<Map.Entry<K, V>>() {
                @Override
                protected Map.Entry<K, V> objectAtIndex(int index) {
                    return new EntryImpl(index);
                }
            };
        }

        @Override
        public int size() {
            return _size;
        }

        @Override
        public boolean contains(Object element) {
            throw new UnsupportedOperationException();
        }
    }

    private class EntryImpl implements Map.Entry<K, V> {
        private final int index;

        EntryImpl(int index) {
            this.index = index;
        }

        @Override
        public K getKey() {
            return (K) _set[index];
        }

        @Override
        public V getValue() {
            return getValueAtIndex(index);
        }

        @Override
        public V setValue(V newValue) {
            V oldValue = getValue();
            setValueAtIndex(index, newValue);
            return oldValue;
        }
    }

    private abstract class THashIterator<E> implements Iterator<E> {
        private int index = capacity();

        protected abstract E objectAtIndex(int index);

        @Override
        public boolean hasNext() {
            return nextIndex() >= 0;
        }

        @Override
        public E next() {
            index = nextIndex();
            if (index < 0) throw new NoSuchElementException();
            return objectAtIndex(index);
        }

        private int nextIndex() {
            Object[] set = _set;
            int i = index;
            while (i-- > 0 && set[i] == null) ;
            return i;
        }
    }
}
