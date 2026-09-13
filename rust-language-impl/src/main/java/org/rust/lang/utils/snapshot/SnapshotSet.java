/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.lang.utils.snapshot;

import jakarta.annotation.Nonnull;

import java.util.*;

public class SnapshotSet<E> extends Snapshotable implements Collection<E> {
    @Nonnull
    private final Set<E> myInner;

    public SnapshotSet() {
        this(new HashSet<>());
    }

    private SnapshotSet(@Nonnull Set<E> inner) {
        myInner = inner;
    }

    @Override
    public boolean add(@Nonnull E element) {
        boolean success = myInner.add(element);
        if (success) {
            myUndoLog.logChange(() -> myInner.remove(element));
        }
        return success;
    }

    @Override
    public int size() {
        return myInner.size();
    }

    @Override
    public boolean isEmpty() {
        return myInner.isEmpty();
    }

    @Override
    public boolean contains(Object o) {
        return myInner.contains(o);
    }

    @Nonnull
    @Override
    public Iterator<E> iterator() {
        return myInner.iterator();
    }

    @Nonnull
    @Override
    public Object[] toArray() {
        return myInner.toArray();
    }

    @Nonnull
    @Override
    public <T> T[] toArray(@Nonnull T[] a) {
        return myInner.toArray(a);
    }

    @Override
    public boolean remove(Object o) {
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean containsAll(@Nonnull Collection<?> c) {
        return myInner.containsAll(c);
    }

    @Override
    public boolean addAll(@Nonnull Collection<? extends E> c) {
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean removeAll(@Nonnull Collection<?> c) {
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean retainAll(@Nonnull Collection<?> c) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void clear() {
        throw new UnsupportedOperationException();
    }
}
