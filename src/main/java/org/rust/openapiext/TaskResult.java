/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.openapiext;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public abstract class TaskResult<T> {
    private TaskResult() {
    }

    public static final class Ok<T> extends TaskResult<T> {
        @NotNull
        private final T value;

        public Ok(@NotNull T value) {
            this.value = value;
        }

        @NotNull
        public T getValue() {
            return value;
        }
    }

    public static final class Err<T> extends TaskResult<T> {
        @NotNull
        private final String reason;
        @Nullable
        private final String message;

        public Err(@NotNull String reason) {
            this(reason, null);
        }

        public Err(@NotNull String reason, @Nullable String message) {
            this.reason = reason;
            this.message = message;
        }

        @NotNull
        public String getReason() {
            return reason;
        }

        @Nullable
        public String getMessage() {
            return message;
        }
    }
}
