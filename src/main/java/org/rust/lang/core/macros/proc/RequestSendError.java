/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.lang.core.macros.proc;

import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.util.concurrent.TimeoutException;

/**
 * Represents errors that can occur when sending a request to the proc macro expander process.
 */
public interface RequestSendError {

    final class ProcessCreation implements RequestSendError {
        @NotNull
        private final ProcessCreationException myException;

        public ProcessCreation(@NotNull ProcessCreationException e) {
            myException = e;
        }

        @NotNull
        public ProcessCreationException getException() {
            return myException;
        }
    }

    final class IO implements RequestSendError {
        @NotNull
        private final IOException myException;

        public IO(@NotNull IOException e) {
            myException = e;
        }

        @NotNull
        public IOException getException() {
            return myException;
        }
    }

    final class Timeout implements RequestSendError {
        @NotNull
        private final TimeoutException myException;

        public Timeout(@NotNull TimeoutException e) {
            myException = e;
        }

        @NotNull
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
