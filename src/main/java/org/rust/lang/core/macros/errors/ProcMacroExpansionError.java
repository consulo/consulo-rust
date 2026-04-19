/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.lang.core.macros.errors;

import org.jetbrains.annotations.NotNull;

public abstract class ProcMacroExpansionError extends MacroExpansionError {
    ProcMacroExpansionError() {}

    @Override
    public String toString() {
        return "ProcMacroExpansionError." + getClass().getSimpleName();
    }

    /** An error occurred on the proc macro expander side. This usually means a panic from a proc-macro */
    public static final class ServerSideError extends ProcMacroExpansionError {
        private final String myMessage;

        public ServerSideError(@NotNull String message) {
            myMessage = message;
        }

        @NotNull
        public String getMessage() {
            return myMessage;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            ServerSideError that = (ServerSideError) o;
            return myMessage.equals(that.myMessage);
        }

        @Override
        public int hashCode() {
            return myMessage.hashCode();
        }

        @Override
        public String toString() {
            return "ProcMacroExpansionError.ServerSideError(message = \"" + myMessage + "\")";
        }
    }

    /**
     * The proc macro expander process exited before answering the request.
     * This can indicate an issue with the procedural macro (a segfault or {@code std::process::exit()} call)
     * or an issue with the proc macro expander process, for example it was killed by a user or OOM-killed.
     */
    public static final class ProcessAborted extends ProcMacroExpansionError {
        private final int myExitCode;

        public ProcessAborted(int exitCode) {
            myExitCode = exitCode;
        }

        public int getExitCode() {
            return myExitCode;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            ProcessAborted that = (ProcessAborted) o;
            return myExitCode == that.myExitCode;
        }

        @Override
        public int hashCode() {
            return Integer.hashCode(myExitCode);
        }

        @Override
        public String toString() {
            return "ProcMacroExpansionError.ProcessAborted(exitCode=" + myExitCode + ")";
        }
    }

    /**
     * An {@link java.io.IOException} thrown during communicating with the proc macro expander
     * (this includes possible OS errors and JSON serialization/deserialization errors).
     */
    public static final ProcMacroExpansionError IOExceptionThrown = new ProcMacroExpansionError() {
        @Override
        public String toString() {
            return "ProcMacroExpansionError.IOExceptionThrown";
        }
    };

    public static final class Timeout extends ProcMacroExpansionError {
        private final long myTimeout;

        public Timeout(long timeout) {
            myTimeout = timeout;
        }

        public long getTimeout() {
            return myTimeout;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            Timeout timeout = (Timeout) o;
            return myTimeout == timeout.myTimeout;
        }

        @Override
        public int hashCode() {
            return Long.hashCode(myTimeout);
        }

        @Override
        public String toString() {
            return "ProcMacroExpansionError.Timeout(timeout=" + myTimeout + ")";
        }
    }

    public static final class UnsupportedExpanderVersion extends ProcMacroExpansionError {
        private final int myVersion;

        public UnsupportedExpanderVersion(int version) {
            myVersion = version;
        }

        public int getVersion() {
            return myVersion;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            UnsupportedExpanderVersion that = (UnsupportedExpanderVersion) o;
            return myVersion == that.myVersion;
        }

        @Override
        public int hashCode() {
            return Integer.hashCode(myVersion);
        }

        @Override
        public String toString() {
            return "ProcMacroExpansionError.UnsupportedExpanderVersion(version=" + myVersion + ")";
        }
    }

    public static final ProcMacroExpansionError CantRunExpander = new ProcMacroExpansionError() {
        @Override
        public String toString() {
            return "ProcMacroExpansionError.CantRunExpander";
        }
    };

    public static final ProcMacroExpansionError ExecutableNotFound = new ProcMacroExpansionError() {
        @Override
        public String toString() {
            return "ProcMacroExpansionError.ExecutableNotFound";
        }
    };

    public static final ProcMacroExpansionError ProcMacroExpansionIsDisabled = new ProcMacroExpansionError() {
        @Override
        public String toString() {
            return "ProcMacroExpansionError.ProcMacroExpansionIsDisabled";
        }
    };
}
