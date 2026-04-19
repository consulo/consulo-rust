/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.lang.utils.snapshot;

import org.jetbrains.annotations.NotNull;

import java.util.*;

public class SnapshotSet<E> extends Snapshotable implements Collection<E> {
    @NotNull
    private final Set<E> myInner;

    public SnapshotSet() {
        this(new HashSet<>());
    }

    private SnapshotSet(@NotNull Set<E> inner) {
        myInner = inner;
    }

    @Override
    public boolean add(@NotNull E element) {
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

    @NotNull
    @Override
    public Iterator<E> iterator() {
        return myInner.iterator();
    }

    @NotNull
    @Override
    public Object[] toArray() {
        return myInner.toArray();
    }

    @NotNull
    @Override
    public <T> T[] toArray(@NotNull T[] a) {
        return myInner.toArray(a);
    }

    @Override
    public boolean remove(Object o) {
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean containsAll(@NotNull Collection<?> c) {
        return myInner.containsAll(c);
    }

    @Override
    public boolean addAll(@NotNull Collection<? extends E> c) {
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean removeAll(@NotNull Collection<?> c) {
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean retainAll(@NotNull Collection<?> c) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void clear() {
        throw new UnsupportedOperationException();
    }
}
