/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.openapiext;

import consulo.process.ExecutionException;
import consulo.process.util.ProcessOutput;
import jakarta.annotation.Nonnull;

public abstract class RsProcessExecutionException extends RsProcessExecutionOrDeserializationException {
    protected RsProcessExecutionException(String message) {
        super(message);
    }

    protected RsProcessExecutionException(Throwable cause) {
        super(cause);
    }

    @Nonnull
    public abstract String getCommandLineString();

    public static class Start extends RsProcessExecutionException {
        @Nonnull
        private final String commandLineString;

        public Start(@Nonnull String commandLineString, @Nonnull ExecutionException cause) {
            super(cause);
            this.commandLineString = commandLineString;
        }

        @Nonnull
        @Override
        public String getCommandLineString() {
            return commandLineString;
        }
    }

    public static class Canceled extends RsProcessExecutionException {
        @Nonnull
        private final String commandLineString;
        @Nonnull
        private final ProcessOutput output;

        public Canceled(@Nonnull String commandLineString, @Nonnull ProcessOutput output) {
            this(commandLineString, output, errorMessage(commandLineString, output));
        }

        public Canceled(@Nonnull String commandLineString, @Nonnull ProcessOutput output, @Nonnull String message) {
            super(message);
            this.commandLineString = commandLineString;
            this.output = output;
        }

        @Nonnull
        @Override
        public String getCommandLineString() {
            return commandLineString;
        }

        @Nonnull
        public ProcessOutput getOutput() {
            return output;
        }
    }

    public static class Timeout extends RsProcessExecutionException {
        @Nonnull
        private final String commandLineString;
        @Nonnull
        private final ProcessOutput output;

        public Timeout(@Nonnull String commandLineString, @Nonnull ProcessOutput output) {
            super(errorMessage(commandLineString, output));
            this.commandLineString = commandLineString;
            this.output = output;
        }

        @Nonnull
        @Override
        public String getCommandLineString() {
            return commandLineString;
        }

        @Nonnull
        public ProcessOutput getOutput() {
            return output;
        }
    }

    /**
     * The process exited with non-zero exit code.
     */
    public static class ProcessAborted extends RsProcessExecutionException {
        @Nonnull
        private final String commandLineString;
        @Nonnull
        private final ProcessOutput output;

        public ProcessAborted(@Nonnull String commandLineString, @Nonnull ProcessOutput output) {
            super(errorMessage(commandLineString, output));
            this.commandLineString = commandLineString;
            this.output = output;
        }

        @Nonnull
        @Override
        public String getCommandLineString() {
            return commandLineString;
        }

        @Nonnull
        public ProcessOutput getOutput() {
            return output;
        }
    }

    @Nonnull
    public static String errorMessage(@Nonnull String commandLineString, @Nonnull ProcessOutput output) {
        return "Execution failed (exit code " + output.getExitCode() + ").\n" +
            commandLineString + "\n" +
            "stdout : " + output.getStdout() + "\n" +
            "stderr : " + output.getStderr();
    }
}
