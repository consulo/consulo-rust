/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.stdext;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.function.Function;

public abstract class RsResult<T, E> {
    private RsResult() {
    }

    public abstract boolean isOk();
    public abstract boolean isErr();

    @Nullable
    public abstract T ok();

    @Nullable
    public abstract E err();

    @NotNull
    public abstract <U> RsResult<U, E> map(@NotNull Function<T, U> mapper);

    @NotNull
    public abstract <U> RsResult<T, U> mapErr(@NotNull Function<E, U> mapper);

    @NotNull
    public abstract T unwrap();

    @NotNull
    public abstract <U> RsResult<U, E> andThen(@NotNull Function<T, RsResult<U, E>> action);

    @NotNull
    public abstract T unwrapOrElse(@NotNull Function<E, T> op);

    public static final class Ok<T, E> extends RsResult<T, E> {
        private final T myOk;

        public Ok(@NotNull T ok) {
            this.myOk = ok;
        }

        public T getOk() {
            return myOk;
        }

        /** Alias for {@link #getOk()} for compatibility. */
        public T get() {
            return myOk;
        }

        @Override public boolean isOk() { return true; }
        @Override public boolean isErr() { return false; }

        @Nullable @Override public T ok() { return myOk; }
        @Nullable @Override public E err() { return null; }

        @NotNull
        @Override
        public <U> RsResult<U, E> map(@NotNull Function<T, U> mapper) {
            return new Ok<>(mapper.apply(myOk));
        }

        @NotNull
        @Override
        @SuppressWarnings("unchecked")
        public <U> RsResult<T, U> mapErr(@NotNull Function<E, U> mapper) {
            return (RsResult<T, U>) this;
        }

        @NotNull
        @Override
        public T unwrap() { return myOk; }

        @NotNull
        @Override
        public <U> RsResult<U, E> andThen(@NotNull Function<T, RsResult<U, E>> action) {
            return action.apply(myOk);
        }

        @NotNull
        @Override
        public T unwrapOrElse(@NotNull Function<E, T> op) {
            return myOk;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (!(o instanceof Ok)) return false;
            Ok<?, ?> ok = (Ok<?, ?>) o;
            return myOk.equals(ok.myOk);
        }

        @Override
        public int hashCode() {
            return myOk.hashCode();
        }

        @Override
        public String toString() {
            return "Ok(" + myOk + ")";
        }
    }

    public static final class Err<T, E> extends RsResult<T, E> {
        private final E myErr;

        public Err(@NotNull E err) {
            this.myErr = err;
        }

        public E getErr() {
            return myErr;
        }

        /** Alias for {@link #getErr()} for compatibility. */
        public E get() {
            return myErr;
        }

        @Override public boolean isOk() { return false; }
        @Override public boolean isErr() { return true; }

        @Nullable @Override public T ok() { return null; }
        @Nullable @Override public E err() { return myErr; }

        @NotNull
        @Override
        @SuppressWarnings("unchecked")
        public <U> RsResult<U, E> map(@NotNull Function<T, U> mapper) {
            return (RsResult<U, E>) this;
        }

        @NotNull
        @Override
        public <U> RsResult<T, U> mapErr(@NotNull Function<E, U> mapper) {
            return new Err<>(mapper.apply(myErr));
        }

        @NotNull
        @Override
        public T unwrap() {
            if (myErr instanceof Throwable) {
                throw new IllegalStateException("called `RsResult.unwrap()` on an `Err` value", (Throwable) myErr);
            } else {
                throw new IllegalStateException("called `RsResult.unwrap()` on an `Err` value: " + myErr);
            }
        }

        @NotNull
        @Override
        @SuppressWarnings("unchecked")
        public <U> RsResult<U, E> andThen(@NotNull Function<T, RsResult<U, E>> action) {
            return (RsResult<U, E>) this;
        }

        @NotNull
        @Override
        public T unwrapOrElse(@NotNull Function<E, T> op) {
            return op.apply(myErr);
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (!(o instanceof Err)) return false;
            Err<?, ?> err = (Err<?, ?>) o;
            return myErr.equals(err.myErr);
        }

        @Override
        public int hashCode() {
            return myErr.hashCode();
        }

        @Override
        public String toString() {
            return "Err(" + myErr + ")";
        }
    }

    /**
     * Unwrap the ok value or throw if the error is a RuntimeException/Error.
     * For non-RuntimeException errors, wraps in IllegalStateException.
     */
    public static <T, E> T unwrapOrThrow(@NotNull RsResult<T, E> result) {
        if (result instanceof Ok) {
            return result.unwrap();
        }
        E err = result.err();
        if (err instanceof RuntimeException) {
            throw (RuntimeException) err;
        } else if (err instanceof Error) {
            throw (Error) err;
        } else if (err instanceof Throwable) {
            throw new RuntimeException((Throwable) err);
        } else {
            throw new IllegalStateException("called unwrapOrThrow on Err: " + err);
        }
    }

    @Nullable
    public static <T> RsResult<T, Unit> toResult(@Nullable T value) {
        if (value != null) {
            return new Ok<>(value);
        }
        return new Err<>(Unit.INSTANCE);
    }

    public static final class Unit {
        public static final Unit INSTANCE = new Unit();
        private Unit() {}
    }
}
