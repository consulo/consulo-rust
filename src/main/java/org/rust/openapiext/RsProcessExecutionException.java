/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.openapiext;

import com.intellij.execution.ExecutionException;
import com.intellij.execution.process.ProcessOutput;
import org.jetbrains.annotations.NotNull;

public abstract class RsProcessExecutionException extends RsProcessExecutionOrDeserializationException {
    protected RsProcessExecutionException(String message) {
        super(message);
    }

    protected RsProcessExecutionException(Throwable cause) {
        super(cause);
    }

    @NotNull
    public abstract String getCommandLineString();

    public static class Start extends RsProcessExecutionException {
        @NotNull
        private final String commandLineString;

        public Start(@NotNull String commandLineString, @NotNull ExecutionException cause) {
            super(cause);
            this.commandLineString = commandLineString;
        }

        @NotNull
        @Override
        public String getCommandLineString() {
            return commandLineString;
        }
    }

    public static class Canceled extends RsProcessExecutionException {
        @NotNull
        private final String commandLineString;
        @NotNull
        private final ProcessOutput output;

        public Canceled(@NotNull String commandLineString, @NotNull ProcessOutput output) {
            this(commandLineString, output, errorMessage(commandLineString, output));
        }

        public Canceled(@NotNull String commandLineString, @NotNull ProcessOutput output, @NotNull String message) {
            super(message);
            this.commandLineString = commandLineString;
            this.output = output;
        }

        @NotNull
        @Override
        public String getCommandLineString() {
            return commandLineString;
        }

        @NotNull
        public ProcessOutput getOutput() {
            return output;
        }
    }

    public static class Timeout extends RsProcessExecutionException {
        @NotNull
        private final String commandLineString;
        @NotNull
        private final ProcessOutput output;

        public Timeout(@NotNull String commandLineString, @NotNull ProcessOutput output) {
            super(errorMessage(commandLineString, output));
            this.commandLineString = commandLineString;
            this.output = output;
        }

        @NotNull
        @Override
        public String getCommandLineString() {
            return commandLineString;
        }

        @NotNull
        public ProcessOutput getOutput() {
            return output;
        }
    }

    /**
     * The process exited with non-zero exit code.
     */
    public static class ProcessAborted extends RsProcessExecutionException {
        @NotNull
        private final String commandLineString;
        @NotNull
        private final ProcessOutput output;

        public ProcessAborted(@NotNull String commandLineString, @NotNull ProcessOutput output) {
            super(errorMessage(commandLineString, output));
            this.commandLineString = commandLineString;
            this.output = output;
        }

        @NotNull
        @Override
        public String getCommandLineString() {
            return commandLineString;
        }

        @NotNull
        public ProcessOutput getOutput() {
            return output;
        }
    }

    @NotNull
    public static String errorMessage(@NotNull String commandLineString, @NotNull ProcessOutput output) {
        return "Execution failed (exit code " + output.getExitCode() + ").\n" +
            commandLineString + "\n" +
            "stdout : " + output.getStdout() + "\n" +
            "stderr : " + output.getStderr();
    }
}
