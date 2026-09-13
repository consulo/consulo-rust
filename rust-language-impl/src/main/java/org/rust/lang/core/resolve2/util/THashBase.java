/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.lang.core.resolve2.util;

import consulo.util.collection.ArrayUtil;
import jakarta.annotation.Nonnull;

/**
 * Open-addressing hash table over a single {@code Object[]} of keys.
 * <p>
 * The protected contract ({@link #_set}, {@link #_size}, {@link #index}, {@link #insertionIndex},
 * {@link #postInsertHook}, {@link #setUp}, {@link #rehash}) is what {@link THashMapBase} and its
 * subclasses build on.
 * <p>
 * Deletion is not supported — that is what lets a probe stop at the first {@code null} slot without
 * tombstone bookkeeping.
 */
public abstract class THashBase<K> {

    protected static final int DEFAULT_INITIAL_CAPACITY = 16;

    /** Trove's default; kept low because probing degrades sharply past ~0.7. */
    private static final float LOAD_FACTOR = 0.5f;

    /** Key slots. {@code null} marks a free slot. Parallel value arrays live in subclasses. */
    protected Object[] _set = ArrayUtil.EMPTY_OBJECT_ARRAY;

    /** Number of occupied slots. */
    protected int _size;

    private int _maxSize;

    public int capacity() {
        return _set.length;
    }

    /**
     * Allocates the key array. Subclasses override to allocate their parallel value arrays, and must
     * call {@code super.setUp(...)} first. Never called from a constructor, so subclass field
     * initializers cannot clobber the arrays allocated here.
     *
     * @return the capacity actually allocated, which may exceed {@code initialCapacity}
     */
    protected int setUp(int initialCapacity) {
        int capacity = tableSizeFor(initialCapacity);
        _set = new Object[capacity];
        computeMaxSize(capacity);
        return capacity;
    }

    /** Subclasses re-insert every key/value pair into arrays of {@code newCapacity}. */
    protected abstract void rehash(int newCapacity);

    /**
     * @return index of {@code key}, or -1 when absent
     */
    protected int index(K key) {
        int length = _set.length;
        if (length == 0) return -1;
        int i = hashIndex(key, length);
        while (true) {
            Object cur = _set[i];
            if (cur == null) return -1;
            if (cur == key || cur.equals(key)) return i;
            i = (i + 1) & (length - 1);
        }
    }

    /**
     * @return the free slot {@code key} should be stored at, or {@code -existingIndex - 1} (always
     * negative) when the key is already present
     */
    protected int insertionIndex(K key) {
        if (_set.length == 0) {
            setUp(DEFAULT_INITIAL_CAPACITY);
        }
        int length = _set.length;
        int i = hashIndex(key, length);
        while (true) {
            Object cur = _set[i];
            if (cur == null) return i;
            if (cur == key || cur.equals(key)) return -i - 1;
            i = (i + 1) & (length - 1);
        }
    }

    protected boolean contains(Object key) {
        @SuppressWarnings("unchecked")
        K typed = (K) key;
        return index(typed) >= 0;
    }

    /**
     * Grows the table once the load factor is exceeded. Callers pass {@code false} only when they
     * reused an existing slot, which cannot happen while deletion is unsupported.
     */
    protected void postInsertHook(boolean usedFreeSlot) {
        if (++_size > _maxSize) {
            int newCapacity = Math.max(_set.length << 1, DEFAULT_INITIAL_CAPACITY);
            rehash(newCapacity);
            computeMaxSize(capacity());
        }
    }

    protected void ensureCapacity(int desiredSize) {
        if (desiredSize <= _maxSize) return;
        int newCapacity = tableSizeFor((int) ((_size + desiredSize) / LOAD_FACTOR) + 1);
        if (_set.length == 0) {
            setUp(newCapacity);
        }
        else {
            rehash(newCapacity);
            computeMaxSize(capacity());
        }
    }

    protected void removeAt(int index) {
        throw new UnsupportedOperationException("deletion is not supported");
    }

    protected static void throwObjectContractViolation(Object existing, Object inserted) {
        throw new IllegalArgumentException(
            "Equal objects must have equal hashcodes. During rehashing, an equal element was found: "
                + existing + " and " + inserted);
    }

    private void computeMaxSize(int capacity) {
        _maxSize = (int) (capacity * LOAD_FACTOR);
    }

    /**
     * Supplemental spread, as in {@code java.util.HashMap} — capacities are powers of two, so
     * without it only the low bits of {@code hashCode()} would select a slot.
     */
    private static int hashIndex(@Nonnull Object key, int length) {
        int h = key.hashCode();
        h ^= (h >>> 20) ^ (h >>> 12);
        h ^= (h >>> 7) ^ (h >>> 4);
        return h & (length - 1);
    }

    private static int tableSizeFor(int capacity) {
        int result = DEFAULT_INITIAL_CAPACITY;
        while (result < capacity) {
            result <<= 1;
        }
        return result;
    }
}
