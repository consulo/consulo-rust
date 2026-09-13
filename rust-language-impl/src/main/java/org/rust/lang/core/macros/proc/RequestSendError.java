/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.lang.core.macros.proc;

import jakarta.annotation.Nonnull;

import java.io.IOException;
import java.util.concurrent.TimeoutException;

/**
 * Represents errors that can occur when sending a request to the proc macro expander process.
 */
public interface RequestSendError {

    final class ProcessCreation implements RequestSendError {
        @Nonnull
        private final ProcessCreationException myException;

        public ProcessCreation(@Nonnull ProcessCreationException e) {
            myException = e;
        }

        @Nonnull
        public ProcessCreationException getException() {
            return myException;
        }
    }

    final class IO implements RequestSendError {
        @Nonnull
        private final IOException myException;

        public IO(@Nonnull IOException e) {
            myException = e;
        }

        @Nonnull
        public IOException getException() {
            return myException;
        }
    }

    final class Timeout implements RequestSendError {
        @Nonnull
        private final TimeoutException myException;

        public Timeout(@Nonnull TimeoutException e) {
            myException = e;
        }

        @Nonnull
        public TimeoutException getException() {
            return myException;
        }
    }

    final class UnknownVersion implements RequestSendError {
        private final int myVersion;

        public UnknownVersion(int version) {
            myVersion = version;
        }

        public int getVersion() {
            return myVersion;
        }
    }
}
