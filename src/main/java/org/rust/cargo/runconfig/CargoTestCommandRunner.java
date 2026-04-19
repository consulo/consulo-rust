/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.cargo.runconfig;

import com.intellij.execution.RunContentExecutor;
import com.intellij.execution.configurations.RunProfile;
import com.intellij.execution.configurations.RunProfileState;
import com.intellij.execution.configurations.RunnerSettings;
import com.intellij.execution.executors.DefaultRunExecutor;
import com.intellij.execution.filters.Filter;
import com.intellij.execution.process.ProcessAdapter;
import com.intellij.execution.process.ProcessEvent;
import com.intellij.execution.process.ProcessHandler;
import com.intellij.execution.runners.AsyncProgramRunner;
import com.intellij.execution.runners.ExecutionEnvironment;
import com.intellij.execution.runners.RunContentBuilder;
import com.intellij.execution.ui.RunContentDescriptor;
import com.intellij.openapi.application.ApplicationManager;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.concurrency.AsyncPromise;
import org.jetbrains.concurrency.Promise;
import org.rust.cargo.runconfig.buildtool.CargoBuildManager;
import org.rust.cargo.runconfig.buildtool.Utils;
import org.rust.cargo.runconfig.command.CargoCommandConfiguration;

import java.util.List;

public class CargoTestCommandRunner extends AsyncProgramRunner<RunnerSettings> {

    public static final String RUNNER_ID = "CargoTestCommandRunner";

    @NotNull
    @Override
    public String getRunnerId() {
        return RUNNER_ID;
    }

    @Override
    public boolean canRun(@NotNull String executorId, @NotNull RunProfile profile) {
        if (!DefaultRunExecutor.EXECUTOR_ID.equals(executorId) || !(profile instanceof CargoCommandConfiguration)) {
            return false;
        }
        CargoCommandConfiguration config = (CargoCommandConfiguration) profile;
        CargoCommandConfiguration.CleanConfiguration.Ok cleaned = config.clean().getOk();
        if (cleaned == null) return false;
        boolean isLocalRun = !RunConfigUtil.getHasRemoteTarget(config) || config.getBuildTarget().isRemote();
        boolean isLegacyTestRun = !CargoBuildManager.INSTANCE.isBuildToolWindowAvailable(config) &&
            List.of("test", "bench").contains(cleaned.getCmd().getCommand()) &&
            CargoBuildManager.INSTANCE.getBuildConfiguration(config) != null;
        return isLocalRun && isLegacyTestRun;
    }

    @NotNull
    @Override
    public Promise<RunContentDescriptor> execute(@NotNull ExecutionEnvironment environment, @NotNull RunProfileState state) {
        org.rust.openapiext.OpenApiUtil.saveAllDocuments();
        boolean onlyBuild = ((CargoRunStateBase) state).getCommandLine().getAdditionalArguments().contains("--no-run");
        return buildTests(environment, (CargoRunStateBase) state, onlyBuild)
            .then(exitCode -> {
                try {
                    if (onlyBuild || exitCode == null || exitCode != 0) return null;
                    com.intellij.execution.ExecutionResult executionResult = state.execute(environment.getExecutor(), CargoTestCommandRunner.this);
                    if (executionResult == null) return null;
                    return new RunContentBuilder(executionResult, environment).showRunContent(environment.getContentToReuse());
                } catch (com.intellij.execution.ExecutionException e) {
                    throw new RuntimeException(e);
                }
            });
    }

    @NotNull
    private static Promise<Integer> buildTests(
        @NotNull ExecutionEnvironment environment,
        @NotNull CargoRunStateBase state,
        boolean cmdHasNoRun
    ) {
        ProcessHandler buildProcessHandler;
        {
            CargoCommandConfiguration.CleanConfiguration.Ok buildConfig;
            org.rust.cargo.toolchain.CargoCommandLine buildCmd = state.getCommandLine().copy(
                state.getCommandLine().getCommand(),
                state.getCommandLine().getWorkingDirectory(),
                state.getCommandLine().getAdditionalArguments(),
                state.getCommandLine().getRedirectInputFrom(),
                false, // emulateTerminal
                state.getCommandLine().getBacktraceMode(),
                state.getCommandLine().getToolchain(),
                state.getCommandLine().getChannel(),
                state.getCommandLine().getEnvironmentVariables(),
                state.getCommandLine().getRequiredFeatures(),
                state.getCommandLine().getAllFeatures(),
                false  // withSudo
            );
            if (!cmdHasNoRun) {
                buildCmd = buildCmd.prependArgument("--no-run");
            }
            buildConfig = new CargoCommandConfiguration.CleanConfiguration.Ok(buildCmd, state.getConfig().getToolchain());
            CargoRunState buildState = new CargoRunState(state.getEnvironment(), state.getRunConfiguration(), buildConfig);
            try {
                buildProcessHandler = buildState.startProcess(true);
            } catch (com.intellij.execution.ExecutionException e) {
                throw new RuntimeException(e);
            }
        }
        AsyncPromise<Integer> exitCode = new AsyncPromise<>();

        if (Utils.isActivateToolWindowBeforeRun(environment) && !org.rust.openapiext.OpenApiUtil.isUnitTestMode()) {
            RunContentExecutor executor = new RunContentExecutor(environment.getProject(), buildProcessHandler);
            for (Filter filter : RunConfigUtil.createFilters(state.getCargoProject())) {
                executor.withFilter(filter);
            }
            executor.withAfterCompletion(() -> exitCode.setResult(buildProcessHandler.getExitCode()));
            executor.run();
        } else {
            buildProcessHandler.addProcessListener(new ProcessAdapter() {
                @Override
                public void processTerminated(@NotNull ProcessEvent event) {
                    ApplicationManager.getApplication().invokeLater(() ->
                        exitCode.setResult(buildProcessHandler.getExitCode())
                    );
                }
            });
            buildProcessHandler.startNotify();
        }
        return exitCode;
    }
}
