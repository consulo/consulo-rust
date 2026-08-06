/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.cargo.runconfig.legacy;

import consulo.execution.DefaultExecutionResult;
import consulo.execution.RunContentExecutor;
import consulo.process.cmd.GeneralCommandLine;
import consulo.execution.configuration.RunProfile;
import consulo.execution.configuration.RunProfileState;
import consulo.execution.configuration.RunnerSettings;
import consulo.process.util.CapturingProcessAdapter;
import consulo.process.util.ProcessOutput;
import consulo.execution.runner.AsyncProgramRunner;
import consulo.execution.runner.ExecutionEnvironment;
import consulo.execution.ExecutionUtil;
import consulo.execution.ui.RunContentDescriptor;
import consulo.application.ApplicationManager;
import consulo.logging.Logger;
import consulo.application.progress.ProgressIndicator;
import consulo.application.progress.Task;
import consulo.project.Project;
import consulo.ui.ex.awt.Messages;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import consulo.util.concurrent.AsyncPromise;
import consulo.util.concurrent.Promise;
import org.rust.RsBundle;
import org.rust.cargo.runconfig.BuildResult;
import org.rust.cargo.runconfig.CargoRunStateBase;
import org.rust.cargo.runconfig.RsProcessHandler;
import org.rust.cargo.runconfig.RsCapturingProcessHandler;
import org.rust.cargo.runconfig.buildtool.CargoBuildManager;
import org.rust.cargo.runconfig.command.CargoCommandConfiguration;
import org.rust.cargo.runconfig.RunConfigUtil;
import org.rust.cargo.runconfig.target.TargetUtil;
import org.rust.cargo.toolchain.CargoCommandLine;
import org.rust.cargo.toolchain.impl.CargoMetadata;
import org.rust.cargo.toolchain.impl.RustcMessage.CompilerArtifactMessage;
import org.rust.cargo.toolchain.tools.Cargo;
import org.rust.cargo.toolchain.wsl.RsWslToolchain;
import org.rust.cargo.util.CargoArgsParser;
import org.rust.cargo.util.ParsedCargoArgs;
import org.rust.openapiext.JsonUtils;
import org.rust.stdext.RsResult;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.List;


/**
 * This runner is used if {@link CargoBuildManager#isBuildToolWindowAvailable} is false.
 */
public abstract class RsAsyncRunner extends AsyncProgramRunner<RunnerSettings> {

    private static final Logger LOG = Logger.getInstance(RsAsyncRunner.class);

    private final String myExecutorId;
    @SuppressWarnings("UnstableApiUsage")
    private final String myErrorMessageTitle;

    protected RsAsyncRunner(@Nonnull String executorId,
                            @SuppressWarnings("UnstableApiUsage")  @Nonnull String errorMessageTitle) {
        myExecutorId = executorId;
        myErrorMessageTitle = errorMessageTitle;
    }

    @Override
    public boolean canRun(@Nonnull String executorId, @Nonnull RunProfile profile) {
        if (!executorId.equals(myExecutorId) || !(profile instanceof CargoCommandConfiguration)) return false;
        CargoCommandConfiguration config = (CargoCommandConfiguration) profile;
        if (!(config.clean() instanceof CargoCommandConfiguration.CleanConfiguration.Ok)) return false;
        return !RunConfigUtil.getHasRemoteTarget(config) &&
            !CargoBuildManager.INSTANCE.isBuildToolWindowAvailable(config) &&
            !CargoBuildManager.INSTANCE.isBuildConfiguration(config) &&
            CargoBuildManager.INSTANCE.getBuildConfiguration(config) != null;
    }

    @Nonnull
    @Override
    protected java.util.concurrent.CompletableFuture<RunContentDescriptor> executeImpl(@Nonnull RunProfileState state, @Nonnull ExecutionEnvironment environment) {
        org.rust.openapiext.OpenApiUtil.saveAllDocuments();

        CargoRunStateBase cargoState = (CargoRunStateBase) state;

        CargoCommandLine commandLine = cargoState.prepareCommandLine(Cargo.getCargoCommonPatch(environment.getProject()));
        ParsedCargoArgs parsedArgs = CargoArgsParser.parseArgs(commandLine.getCommand(), commandLine.getAdditionalArguments());
        List<String> commandArguments = parsedArgs.commandArguments();
        List<String> executableArguments = parsedArgs.executableArguments();
        List<String> additionalBuildArgs = TargetUtil.getLocalBuildArgsForRemoteRun(cargoState.getRunConfiguration());

        boolean isTestRun = commandLine.getCommand().equals("test") || commandLine.getCommand().equals("bench");
        boolean cmdHasNoRun = commandLine.getAdditionalArguments().contains("--no-run");

        CargoCommandLine buildCommand;
        if (isTestRun) {
            if (cmdHasNoRun) {
                buildCommand = commandLine;
            } else {
                buildCommand = commandLine.prependArgument("--no-run");
            }
        } else {
            List<String> buildArgs = new java.util.ArrayList<>(commandArguments);
            buildArgs.addAll(additionalBuildArgs);
            buildCommand = commandLine.copy("build", buildArgs);
        }
        // Simplified: building does not require root privileges
        buildCommand = buildCommand.withSudo(false);

        boolean finalIsTestRun = isTestRun;
        boolean finalCmdHasNoRun = cmdHasNoRun;
        CargoCommandLine finalBuildCommand = buildCommand;
        // Simplified: synchronous run using the build-and-binary helper.
        Object binary;
        try {
            binary = buildProjectAndGetBinaryArtifactPath(environment.getProject(), finalBuildCommand, cargoState, finalIsTestRun).blockingGet(60_000);
        } catch (Throwable t) {
            java.util.concurrent.CompletableFuture<RunContentDescriptor> failed = new java.util.concurrent.CompletableFuture<>();
            failed.completeExceptionally(t);
            return failed;
        }
        if (finalIsTestRun && finalCmdHasNoRun) return java.util.concurrent.CompletableFuture.completedFuture(null);
        if (binary == null) return java.util.concurrent.CompletableFuture.completedFuture(null);
        try {
            Path path = ((Binary) binary).getPath();
            GeneralCommandLine runCommand = cargoState.getToolchain().createGeneralCommandLine(
                path,
                commandLine.getWorkingDirectory(),
                commandLine.getRedirectInputFrom(),
                commandLine.getBacktraceMode(),
                commandLine.getEnvironmentVariables(),
                executableArguments,
                false,
                commandLine.getWithSudo(),
                false,
                null
            );
            return java.util.concurrent.CompletableFuture.completedFuture(
                getRunContentDescriptor(cargoState, environment, runCommand));
        } catch (consulo.process.ExecutionException e) {
            java.util.concurrent.CompletableFuture<RunContentDescriptor> failed = new java.util.concurrent.CompletableFuture<>();
            failed.completeExceptionally(e);
            return failed;
        }
    }

    @Nullable
    protected RunContentDescriptor getRunContentDescriptor(
        @Nonnull CargoRunStateBase state,
        @Nonnull ExecutionEnvironment environment,
        @Nonnull GeneralCommandLine runCommand
    ) throws consulo.process.ExecutionException {
        DefaultExecutionResult executionResult = executeCommandLine(state, runCommand, environment);
        return new consulo.execution.runner.RunContentBuilder(executionResult, environment).showRunContent(environment.getContentToReuse());
    }

    @Nonnull
    private DefaultExecutionResult executeCommandLine(
        @Nonnull CargoRunStateBase state,
        @Nonnull GeneralCommandLine commandLine,
        @Nonnull ExecutionEnvironment environment
    ) throws consulo.process.ExecutionException {
        return RunConfigUtil.executeCommandLine(state, commandLine, environment);
    }

    protected boolean checkToolchainConfigured(@Nonnull Project project) {
        return true;
    }

    @Nullable
    protected BuildResult.ToolchainError checkToolchainSupported(@Nonnull Project project, @Nonnull String host) {
        return null;
    }

    protected void processUnsupportedToolchain(
        @Nonnull Project project,
        @Nonnull BuildResult.ToolchainError toolchainError,
        @Nonnull AsyncPromise<Binary> promise
    ) {
        showErrorDialog(project, toolchainError.getMessage());
        promise.setResult(null);
    }

    @Nonnull
    private Promise<Binary> buildProjectAndGetBinaryArtifactPath(
        @Nonnull Project project,
        @Nonnull CargoCommandLine command,
        @Nonnull CargoRunStateBase state,
        boolean isTestBuild
    ) {
        AsyncPromise<Binary> promise = new AsyncPromise<>();
        Object toolchain = state.getToolchain();
        Object cargo = state.cargo();

        ProcessOutput processForUserOutput = new ProcessOutput();
        GeneralCommandLine commandLine = ((org.rust.cargo.toolchain.tools.Cargo) cargo).toColoredCommandLine(project, command);
        LOG.debug("Executing Cargo command: `" + commandLine.getCommandLineString() + "`");
        RsProcessHandler processForUser;
        try {
            processForUser = new RsProcessHandler(commandLine);
        } catch (consulo.process.ExecutionException e) {
            throw new RuntimeException(e);
        }

        processForUser.addProcessListener(new CapturingProcessAdapter(processForUserOutput));

        ApplicationManager.getApplication().invokeLater(() -> {
            if (!checkToolchainConfigured(project)) {
                promise.setResult(null);
                return;
            }

            RunContentExecutor executor = new RunContentExecutor(project, processForUser);
            for (consulo.execution.ui.console.Filter filter : RunConfigUtil.createFilters(state.getCargoProject())) {
                executor.withFilter(filter);
            }
            executor.withAfterCompletion(() -> {
                if (processForUserOutput.getExitCode() != 0) {
                    promise.setResult(null);
                    return;
                }

                new Task.Backgroundable(project, RsBundle.message("progress.title.building.cargo.project")) {
                    BuildResult result = null;

                    @Override
                    public void run(@Nonnull ProgressIndicator indicator) {
                        indicator.setIndeterminate(true);
                        org.rust.cargo.toolchain.impl.RustcVersion rustVersion = state.rustVersion();
                        String host = rustVersion != null && rustVersion.getHost() != null ? rustVersion.getHost() : "";
                        result = checkToolchainSupported(project, host);
                        if (result != null) return;

                        CargoCommandLine jsonCommand = command.prependArgument("--message-format=json");
                        RsResult<RsCapturingProcessHandler, ?> handlerResult = RsCapturingProcessHandler.startProcess(
                            ((org.rust.cargo.toolchain.tools.Cargo) cargo).toGeneralCommandLine(project, jsonCommand)
                        );
                        consulo.process.internal.CapturingProcessHandler processForJson = ((RsCapturingProcessHandler) handlerResult.unwrap()).getDelegate();
                        ProcessOutput output = processForJson.runProcessWithProgressIndicator(indicator);
                        if (output.isCancelled() || output.getExitCode() != 0) {
                            promise.setResult(null);
                            return;
                        }

                        java.util.List<String> binaries = new java.util.ArrayList<>();
                        for (String line : output.getStdoutLines()) {
                            com.google.gson.JsonObject json = JsonUtils.tryParseJsonObject(line);
                            if (json == null) continue;
                            CompilerArtifactMessage artifact = CompilerArtifactMessage.fromJson(json);
                            if (artifact == null) continue;

                            CargoMetadata.Target target = artifact.getTarget();
                            org.rust.cargo.toolchain.impl.RustcMessage.Profile profile = artifact.getProfile();

                            boolean isSuitableTarget;
                            switch (target.getCleanKind()) {
                                case BIN:
                                    isSuitableTarget = true;
                                    break;
                                case EXAMPLE:
                                    isSuitableTarget = target.getCleanCrateTypes().size() == 1 &&
                                        target.getCleanCrateTypes().get(0) == CargoMetadata.CrateType.BIN;
                                    break;
                                case TEST:
                                case BENCH:
                                    isSuitableTarget = true;
                                    break;
                                case LIB:
                                    isSuitableTarget = profile.isTest();
                                    break;
                                default:
                                    isSuitableTarget = false;
                                    break;
                            }
                            if (isSuitableTarget && (!isTestBuild || profile.isTest())) {
                                binaries.addAll(artifact.getExecutables());
                            }
                        }
                        result = new BuildResult.Binaries(binaries);
                    }

                    @Override
                    public void onSuccess() {
                        if (result instanceof BuildResult.ToolchainError) {
                            processUnsupportedToolchain(project, (BuildResult.ToolchainError) result, promise);
                        } else if (result instanceof BuildResult.Binaries) {
                            List<String> paths = ((BuildResult.Binaries) result).getPaths();
                            if (paths.isEmpty()) {
                                showErrorDialog(project, RsBundle.message("dialog.message.can.t.find.binary"));
                                promise.setResult(null);
                            } else if (paths.size() > 1) {
                                showErrorDialog(project,
                                    RsBundle.message("dialog.message.more.than.one.binary.was.produced.please.specify.bin.lib.test.or.example.flag.explicitly"));
                                promise.setResult(null);
                            } else {
                                promise.setResult(new Binary(Paths.get(paths.get(0))));
                            }
                        }
                    }

                    @Override
                    public void onThrowable(@Nonnull Throwable error) {
                        promise.setResult(null);
                    }
                }.queue();
            }).run();
        });

        return promise;
    }

    protected void showErrorDialog(@Nonnull Project project,
                                   @SuppressWarnings("UnstableApiUsage")  @Nonnull String message) {
        Messages.showErrorDialog(project, message, myErrorMessageTitle);
    }

    public static class Binary {
        private final Path myPath;

        public Binary(@Nonnull Path path) {
            myPath = path;
        }

        @Nonnull
        public Path getPath() {
            return myPath;
        }
    }
}
