/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.cargo.runconfig.wasmpack;

import consulo.execution.configuration.CommandLineState;
import consulo.process.cmd.GeneralCommandLine;
import consulo.process.ProcessHandler;
import consulo.execution.process.ProcessTerminatedListener;
import consulo.execution.runner.ExecutionEnvironment;
import consulo.process.cmd.ParametersListUtil;
import jakarta.annotation.Nonnull;
import org.rust.cargo.runconfig.RsProcessHandler;
import org.rust.cargo.runconfig.console.RsConsoleBuilder;
import org.rust.cargo.toolchain.tools.WasmPack;

import java.io.File;
import java.util.List;

public class WasmPackCommandRunState extends CommandLineState {

    @Nonnull
    private final WasmPackCommandConfiguration runConfiguration;
    @Nonnull
    private final WasmPack wasmPack;
    @Nonnull
    private final File workingDirectory;

    public WasmPackCommandRunState(
        @Nonnull ExecutionEnvironment environment,
        @Nonnull WasmPackCommandConfiguration runConfiguration,
        @Nonnull WasmPack wasmPack,
        @Nonnull File workingDirectory
    ) {
        super(environment);
        this.runConfiguration = runConfiguration;
        this.wasmPack = wasmPack;
        this.workingDirectory = workingDirectory;
        setConsoleBuilder(new RsConsoleBuilder(environment.getProject(), runConfiguration));
    }

    @Nonnull
    @Override
    protected ProcessHandler startProcess() throws consulo.process.ExecutionException {
        List<String> params = ParametersListUtil.parse(runConfiguration.getCommand());
        String command = params.isEmpty() ? "" : params.get(0);
        List<String> restParams = params.size() > 1 ? params.subList(1, params.size()) : List.of();

        GeneralCommandLine commandLine = wasmPack.createCommandLine(
            workingDirectory,
            command,
            restParams,
            runConfiguration.getEmulateTerminal()
        );

        RsProcessHandler handler = new RsProcessHandler(commandLine);
        ProcessTerminatedListener.attach(handler); // shows exit code upon termination
        return handler;
    }
}
