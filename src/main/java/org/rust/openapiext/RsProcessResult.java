/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.openapiext;

import com.intellij.execution.process.ProcessOutput;
import org.jetbrains.annotations.NotNull;
import org.rust.stdext.RsResult;

/**
 * Utility methods for {@code RsResult<ProcessOutput, RsProcessExecutionException>}
 *
 * <p>This class also serves as a holder for the sealed class hierarchy and typealias
 * <ul>
 *   <li>{@link RsProcessExecutionOrDeserializationException}</li>
 *   <li>{@link RsDeserializationException}</li>
 *   <li>{@link RsProcessExecutionException} (with subclasses Start, Canceled, Timeout, ProcessAborted)</li>
 * </ul>
 */
public final class RsProcessResult {
    private RsProcessResult() {
    }

    /**
     * Converts any non-Start error into an Ok result containing the output.
     * Only Start errors are preserved as errors.
     */
    @NotNull
    public static RsResult<ProcessOutput, RsProcessExecutionException.Start> ignoreExitCode(
        @NotNull RsResult<ProcessOutput, RsProcessExecutionException> result
    ) {
        if (result.isOk()) {
            return new RsResult.Ok<>(result.unwrap());
        }
        RsProcessExecutionException err = result.err();
        if (err instanceof RsProcessExecutionException.Start) {
            return new RsResult.Err<>((RsProcessExecutionException.Start) err);
        } else if (err instanceof RsProcessExecutionException.Canceled) {
            return new RsResult.Ok<>(((RsProcessExecutionException.Canceled) err).getOutput());
        } else if (err instanceof RsProcessExecutionException.Timeout) {
            return new RsResult.Ok<>(((RsProcessExecutionException.Timeout) err).getOutput());
        } else if (err instanceof RsProcessExecutionException.ProcessAborted) {
            return new RsResult.Ok<>(((RsProcessExecutionException.ProcessAborted) err).getOutput());
        } else {
            // Should not happen since the hierarchy is sealed, but handle gracefully
            throw new IllegalStateException("Unknown RsProcessExecutionException type: " + err.getClass());
        }
    }
}
