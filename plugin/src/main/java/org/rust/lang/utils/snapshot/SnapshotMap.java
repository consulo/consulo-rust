/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.lang.utils.snapshot;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

public class SnapshotMap<K, V> extends Snapshotable {
    @Nonnull
    private final Map<K, V> myInner = new HashMap<>();

    public int getSize() {
        return myInner.size();
    }

    public boolean isEmpty() {
        return myInner.isEmpty();
    }

    @Nonnull
    public Iterator<Map.Entry<K, V>> iterator() {
        return myInner.entrySet().iterator();
    }

    public boolean contains(@Nonnull K key) {
        return myInner.containsKey(key);
    }

    @Nullable
    public V get(@Nonnull K key) {
        return myInner.get(key);
    }

    public void set(@Nonnull K key, @Nonnull V value) {
        put(key, value);
    }

    @Nullable
    public V put(@Nonnull K key, @Nonnull V value) {
        V oldValue = myInner.put(key, value);
        if (oldValue == null) {
            myUndoLog.logChange(() -> myInner.remove(key));
        } else {
            myUndoLog.logChange(() -> myInner.put(key, oldValue));
        }
        return oldValue;
    }
}
