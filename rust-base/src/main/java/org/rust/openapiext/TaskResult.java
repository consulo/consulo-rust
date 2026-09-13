/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.openapiext;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

public abstract class TaskResult<T> {
    private TaskResult() {
    }

    public static final class Ok<T> extends TaskResult<T> {
        @Nonnull
        private final T value;

        public Ok(@Nonnull T value) {
            this.value = value;
        }

        @Nonnull
        public T getValue() {
            return value;
        }
    }

    public static final class Err<T> extends TaskResult<T> {
        @Nonnull
        private final String reason;
        @Nullable
        private final String message;

        public Err(@Nonnull String reason) {
            this(reason, null);
        }

        public Err(@Nonnull String reason, @Nullable String message) {
            this.reason = reason;
            this.message = message;
        }

        @Nonnull
        public String getReason() {
            return reason;
        }

        @Nullable
        public String getMessage() {
            return message;
        }
    }
}
