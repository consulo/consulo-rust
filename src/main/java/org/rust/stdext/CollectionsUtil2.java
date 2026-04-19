/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.stdext;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;
import java.util.function.Function;

/**
 * Supplementary collection utilities not covered by {@link CollectionsUtil}.
 */
public final class CollectionsUtil2 {

    private CollectionsUtil2() {
    }

    /**
     * Chains two lists into a single iterable sequence, avoiding unnecessary copies when one is empty.
     */
    @NotNull
    public static <T> Iterable<T> chain(@NotNull List<T> first, @NotNull List<T> other) {
        if (other.isEmpty()) return first;
        if (first.isEmpty()) return other;
        return () -> new Iterator<T>() {
            private final Iterator<T> myFirstIter = first.iterator();
            private final Iterator<T> mySecondIter = other.iterator();

            @Override
            public boolean hasNext() {
                return myFirstIter.hasNext() || mySecondIter.hasNext();
            }

            @Override
            public T next() {
                if (myFirstIter.hasNext()) return myFirstIter.next();
                return mySecondIter.next();
            }
        };
    }

    /**
     * Maps elements to a mutable list (ArrayList).
     */
    @NotNull
    public static <T, R> List<R> mapToMutableList(@NotNull Iterable<T> iterable, @NotNull Function<T, R> transform) {
        int size = CollectionsUtil.collectionSizeOrDefault(iterable, 10);
        List<R> result = new ArrayList<>(size);
        for (T item : iterable) {
            result.add(transform.apply(item));
        }
        return result;
    }

    /**
     * Maps elements to a set.
     */
    @NotNull
    public static <T, R> Set<R> mapToSet(@NotNull Iterable<T> iterable, @NotNull Function<T, R> transform) {
        int size = CollectionsUtil.collectionSizeOrDefault(iterable, 10);
        Set<R> result = new HashSet<>(CollectionsUtil.mapCapacity(size));
        for (T item : iterable) {
            result.add(transform.apply(item));
        }
        return result;
    }

    /**
     * Maps elements to a set, skipping null results.
     */
    @NotNull
    public static <T, R> Set<R> mapNotNullToSet(@NotNull Iterable<T> iterable, @NotNull Function<T, R> transform) {
        int size = CollectionsUtil.collectionSizeOrDefault(iterable, 10);
        Set<R> result = new HashSet<>(CollectionsUtil.mapCapacity(size));
        for (T item : iterable) {
            R mapped = transform.apply(item);
            if (mapped != null) {
                result.add(mapped);
            }
        }
        return result;
    }

    /**
     * Creates an empty EnumSet for the given enum class.
     */
    @NotNull
    public static <E extends Enum<E>> EnumSet<E> enumSetOf(@NotNull Class<E> enumClass) {
        return EnumSet.noneOf(enumClass);
    }

    /**
     * Returns an iterable that pairs each element with its previous element (null for the first).
     * Each pair is (current, previous).
     */
    @NotNull
    public static <T> Iterable<LookbackValue<T>> withPrevious(@NotNull Iterable<T> iterable) {
        return () -> new LookbackIterator<>(iterable.iterator());
    }

    /**
     * Returns an iterable that pairs each element with its next element (null for the last).
     * Each pair is (current, next).
     */
    @NotNull
    public static <T> Iterable<WithNextValue<T>> withNext(@NotNull Iterable<T> iterable) {
        return () -> new WithNextIterator<>(iterable.iterator());
    }

    /**
     * A pair of (current value, previous value). Previous is null for the first element.
     */
    public static final class LookbackValue<T> {
        private final T myCurrent;
        @Nullable
        private final T myPrevious;

        public LookbackValue(@NotNull T current, @Nullable T previous) {
            myCurrent = current;
            myPrevious = previous;
        }

        @NotNull
        public T getCurrent() {
            return myCurrent;
        }

        @Nullable
        public T getPrevious() {
            return myPrevious;
        }
    }

    /**
     * A pair of (current value, next value). Next is null for the last element.
     */
    public static final class WithNextValue<T> {
        @NotNull
        private final T myCurrent;
        @Nullable
        private final T myNext;

        public WithNextValue(@NotNull T current, @Nullable T next) {
            myCurrent = current;
            myNext = next;
        }

        @NotNull
        public T getCurrent() {
            return myCurrent;
        }

        @Nullable
        public T getNext() {
            return myNext;
        }
    }

    private static final class LookbackIterator<T> implements Iterator<LookbackValue<T>> {
        private final Iterator<T> myIterator;
        @Nullable
        private T myPrevious;

        LookbackIterator(@NotNull Iterator<T> iterator) {
            myIterator = iterator;
            myPrevious = null;
        }

        @Override
        public boolean hasNext() {
            return myIterator.hasNext();
        }

        @Override
        public LookbackValue<T> next() {
            T next = myIterator.next();
            LookbackValue<T> result = new LookbackValue<>(next, myPrevious);
            myPrevious = next;
            return result;
        }
    }

    private static final class WithNextIterator<T> implements Iterator<WithNextValue<T>> {
        private final Iterator<T> myIterator;
        @Nullable
        private T myNext;
        private boolean myInitialized;

        WithNextIterator(@NotNull Iterator<T> iterator) {
            myIterator = iterator;
            myInitialized = false;
        }

        @Override
        public boolean hasNext() {
            return myNext != null || myIterator.hasNext();
        }

        @Override
        public WithNextValue<T> next() {
            if (!myInitialized) {
                if (!myIterator.hasNext()) throw new NoSuchElementException();
                myNext = myIterator.next();
                myInitialized = true;
            }
            T current = myNext;
            if (current == null) throw new NoSuchElementException();
            myNext = myIterator.hasNext() ? myIterator.next() : null;
            return new WithNextValue<>(current, myNext);
        }
    }
}
