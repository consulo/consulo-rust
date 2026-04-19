/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.cargo.runconfig;

import com.intellij.execution.ExecutionException;
import com.intellij.execution.configurations.GeneralCommandLine;
import com.intellij.execution.process.CapturingProcessHandler;
import com.intellij.util.io.BaseOutputReader;
import org.jetbrains.annotations.NotNull;
import org.rust.stdext.RsResult;

public class RsCapturingProcessHandler extends CapturingProcessHandler {

    private RsCapturingProcessHandler(@NotNull GeneralCommandLine commandLine) throws ExecutionException {
        super(commandLine);
    }

    @NotNull
    @Override
    protected BaseOutputReader.Options readerOptions() {
        return BaseOutputReader.Options.BLOCKING;
    }

    @NotNull
    public static RsResult<RsCapturingProcessHandler, ExecutionException> startProcess(@NotNull GeneralCommandLine commandLine) {
        try {
            return new RsResult.Ok<>(new RsCapturingProcessHandler(commandLine));
        } catch (ExecutionException e) {
            return new RsResult.Err<>(e);
        }
    }
}
