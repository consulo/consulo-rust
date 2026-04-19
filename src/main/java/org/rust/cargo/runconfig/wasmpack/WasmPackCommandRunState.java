/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.cargo.runconfig.wasmpack;

import com.intellij.execution.configurations.CommandLineState;
import com.intellij.execution.configurations.GeneralCommandLine;
import com.intellij.execution.process.ProcessHandler;
import com.intellij.execution.process.ProcessTerminatedListener;
import com.intellij.execution.runners.ExecutionEnvironment;
import com.intellij.util.execution.ParametersListUtil;
import org.jetbrains.annotations.NotNull;
import org.rust.cargo.runconfig.RsProcessHandler;
import org.rust.cargo.runconfig.console.RsConsoleBuilder;
import org.rust.cargo.toolchain.tools.WasmPack;

import java.io.File;
import java.util.List;

public class WasmPackCommandRunState extends CommandLineState {

    @NotNull
    private final WasmPackCommandConfiguration runConfiguration;
    @NotNull
    private final WasmPack wasmPack;
    @NotNull
    private final File workingDirectory;

    public WasmPackCommandRunState(
        @NotNull ExecutionEnvironment environment,
        @NotNull WasmPackCommandConfiguration runConfiguration,
        @NotNull WasmPack wasmPack,
        @NotNull File workingDirectory
    ) {
        super(environment);
        this.runConfiguration = runConfiguration;
        this.wasmPack = wasmPack;
        this.workingDirectory = workingDirectory;
        setConsoleBuilder(new RsConsoleBuilder(environment.getProject(), runConfiguration));
    }

    @NotNull
    @Override
    protected ProcessHandler startProcess() throws com.intellij.execution.ExecutionException {
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
