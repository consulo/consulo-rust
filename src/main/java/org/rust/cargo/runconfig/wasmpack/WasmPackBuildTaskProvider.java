/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.cargo.runconfig.wasmpack;

import com.intellij.execution.configurations.RunConfiguration;
import com.intellij.execution.runners.ExecutionEnvironment;
import com.intellij.openapi.actionSystem.DataContext;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.Key;
import com.intellij.util.execution.ParametersListUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.rust.cargo.runconfig.buildtool.RsBuildTaskProvider;
import org.rust.cargo.runconfig.command.CargoCommandConfiguration;
import org.rust.cargo.runconfig.command.CargoCommandConfigurationType;
import org.rust.cargo.toolchain.tools.Rustup;
import org.rust.cargo.util.CargoArgsParserUtil;
import org.rust.openapiext.OpenApiUtil;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

public class WasmPackBuildTaskProvider extends RsBuildTaskProvider<WasmPackBuildTaskProvider.BuildTask> {

    public static final Key<BuildTask> ID = Key.create("WASM_PACK.BUILD_TASK_PROVIDER");
    public static final String WASM_TARGET = "wasm32-unknown-unknown";

    @NotNull
    @Override
    public Key<BuildTask> getId() {
        return ID;
    }

    @Nullable
    @Override
    public BuildTask createTask(@NotNull RunConfiguration runConfiguration) {
        if (runConfiguration instanceof WasmPackCommandConfiguration) {
            return new BuildTask();
        }
        return null;
    }

    @Override
    public boolean executeTask(
        @NotNull DataContext context,
        @NotNull RunConfiguration configuration,
        @NotNull ExecutionEnvironment environment,
        @NotNull BuildTask task
    ) {
        if (!(configuration instanceof WasmPackCommandConfiguration)) return false;
        WasmPackCommandConfiguration wasmPackConfig = (WasmPackCommandConfiguration) configuration;

        Project project = org.rust.openapiext.OpenApiUtil.getProject(context);
        if (project == null) return false;
        Path cargoProjectDirectory = wasmPackConfig.getWorkingDirectory();
        if (cargoProjectDirectory == null) return false;
        if (Rustup.checkNeedInstallWasmTarget(project, cargoProjectDirectory)) return false;

        List<String> configurationArgs = ParametersListUtil.parse(wasmPackConfig.getCommand());
        List<List<String>> splitResult = CargoArgsParserUtil.splitOnDoubleDash(configurationArgs);
        List<String> preArgs = splitResult.get(0);
        List<String> postArgs = splitResult.get(1);
        String configurationCommand = configurationArgs.isEmpty() ? null : configurationArgs.get(0);
        if (configurationCommand == null) return false;

        List<String> parameters = new ArrayList<>();
        parameters.add("build");
        parameters.addAll(postArgs);

        if ("test".equals(configurationCommand)) {
            parameters.add("--tests");
        }

        if (!preArgs.contains("--dev")) {
            parameters.add("--release");
        }

        if (!postArgs.contains("--target")) {
            parameters.add("--target");
            parameters.add(WASM_TARGET);
        }

        String buildCommand = ParametersListUtil.join(parameters);

        CargoCommandConfiguration buildConfiguration = new CargoCommandConfiguration(
            project, configuration.getName(), CargoCommandConfigurationType.getInstance().getFactory()
        );
        buildConfiguration.setCommand(buildCommand);
        buildConfiguration.setWorkingDirectory(wasmPackConfig.getWorkingDirectory());
        buildConfiguration.setEmulateTerminal(false);

        return doExecuteTask(buildConfiguration, environment);
    }

    public static class BuildTask extends RsBuildTaskProvider.BuildTask<BuildTask> {
        public BuildTask() {
            super(ID);
        }
    }
}
