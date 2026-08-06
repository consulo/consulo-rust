/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.cargo.runconfig;
import consulo.execution.ExecutionResult;

import consulo.execution.RunContentExecutor;
import consulo.execution.configuration.RunProfile;
import consulo.execution.configuration.RunProfileState;
import consulo.execution.configuration.RunnerSettings;
import consulo.execution.executor.DefaultRunExecutor;
import consulo.execution.ui.console.Filter;
import consulo.process.event.ProcessAdapter;
import consulo.process.event.ProcessEvent;
import consulo.process.ProcessHandler;
import consulo.execution.runner.AsyncProgramRunner;
import consulo.execution.runner.ExecutionEnvironment;
import consulo.execution.runner.RunContentBuilder;
import consulo.execution.ui.RunContentDescriptor;
import consulo.application.ApplicationManager;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import consulo.util.concurrent.AsyncPromise;
import consulo.util.concurrent.Promise;
import org.rust.cargo.runconfig.buildtool.CargoBuildManager;
import org.rust.cargo.runconfig.buildtool.Utils;
import org.rust.cargo.runconfig.command.CargoCommandConfiguration;

import java.util.List;

public class CargoTestCommandRunner extends AsyncProgramRunner<RunnerSettings> {

    public static final String RUNNER_ID = "CargoTestCommandRunner";

    @Nonnull
    @Override
    public String getRunnerId() {
        return RUNNER_ID;
    }

    @Override
    public boolean canRun(@Nonnull String executorId, @Nonnull RunProfile profile) {
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

    @Nonnull
    @Override
    protected java.util.concurrent.CompletableFuture<RunContentDescriptor> executeImpl(@Nonnull RunProfileState state, @Nonnull ExecutionEnvironment environment) {
        org.rust.openapiext.OpenApiUtil.saveAllDocuments();
        boolean onlyBuild = ((CargoRunStateBase) state).getCommandLine().getAdditionalArguments().contains("--no-run");
        java.util.concurrent.CompletableFuture<RunContentDescriptor> result = new java.util.concurrent.CompletableFuture<>();
        buildTests(environment, (CargoRunStateBase) state, onlyBuild).onSuccess(exitCode -> {
            try {
                if (onlyBuild || exitCode == null || exitCode != 0) {
                    result.complete(null);
                    return;
                }
                consulo.execution.ExecutionResult executionResult = state.execute(environment.getExecutor(), CargoTestCommandRunner.this);
                if (executionResult == null) {
                    result.complete(null);
                    return;
                }
                result.complete(new RunContentBuilder(executionResult, environment).showRunContent(environment.getContentToReuse()));
            } catch (consulo.process.ExecutionException e) {
                result.completeExceptionally(e);
            }
        });
        return result;
    }

    @Nonnull
    private static Promise<Integer> buildTests(
        @Nonnull ExecutionEnvironment environment,
        @Nonnull CargoRunStateBase state,
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
            } catch (consulo.process.ExecutionException e) {
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
                public void processTerminated(@Nonnull ProcessEvent event) {
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
