/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.lang.core.resolve;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Collections;
import java.util.List;
import java.util.function.Function;

public abstract class SelectionResult<T> {

    private static final Err ERR_INSTANCE = new Err();

    private SelectionResult() {}

    @Nullable
    @SuppressWarnings("unchecked")
    public T ok() {
        if (this instanceof Ok) {
            return ((Ok<T>) this).getResult();
        }
        return null;
    }

    public boolean isOk() {
        return this instanceof Ok;
    }

    @NotNull
    @SuppressWarnings("unchecked")
    public <R> SelectionResult<R> map(@NotNull Function<T, R> action) {
        if (this instanceof Err) return (SelectionResult<R>) this;
        if (this instanceof Ambiguous) return (SelectionResult<R>) this;
        return new Ok<>(action.apply(((Ok<T>) this).getResult()));
    }

    @NotNull
    @SuppressWarnings("unchecked")
    public <R> SelectionResult<R> andThen(@NotNull Function<T, SelectionResult<R>> action) {
        if (this instanceof Err) return (SelectionResult<R>) this;
        if (this instanceof Ambiguous) return (SelectionResult<R>) this;
        return action.apply(((Ok<T>) this).getResult());
    }

    @NotNull
    @SuppressWarnings("unchecked")
    public static <T> SelectionResult<T> err() {
        return (SelectionResult<T>) ERR_INSTANCE;
    }

    @NotNull
    public static <T> SelectionResult<T> ambiguous() {
        return new Ambiguous<>(Collections.emptyList());
    }

    @NotNull
    public static <T> SelectionResult<T> ambiguous(@NotNull List<SelectionCandidate> candidates) {
        return new Ambiguous<>(candidates);
    }

    @NotNull
    public static <T> SelectionResult<T> ok(@NotNull T result) {
        return new Ok<>(result);
    }

    // --- Subclasses ---

    public static final class Err extends SelectionResult<Object> {
        private Err() {}
    }

    public static final class Ambiguous<T> extends SelectionResult<T> {
        @NotNull
        private final List<SelectionCandidate> candidates;

        public Ambiguous(@NotNull List<SelectionCandidate> candidates) {
            this.candidates = candidates;
        }

        @NotNull
        public List<SelectionCandidate> getCandidates() {
            return candidates;
        }
    }

    public static final class Ok<T> extends SelectionResult<T> {
        @NotNull
        private final T result;

        public Ok(@NotNull T result) {
            this.result = result;
        }

        @NotNull
        public T getResult() {
            return result;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (!(o instanceof Ok)) return false;
            return result.equals(((Ok<?>) o).result);
        }

        @Override
        public int hashCode() {
            return result.hashCode();
        }
    }
}
