/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.cargo.runconfig.legacy;

import com.intellij.execution.DefaultExecutionResult;
import com.intellij.execution.RunContentExecutor;
import com.intellij.execution.configurations.GeneralCommandLine;
import com.intellij.execution.configurations.RunProfile;
import com.intellij.execution.configurations.RunProfileState;
import com.intellij.execution.configurations.RunnerSettings;
import com.intellij.execution.process.CapturingProcessAdapter;
import com.intellij.execution.process.ProcessOutput;
import com.intellij.execution.runners.AsyncProgramRunner;
import com.intellij.execution.runners.ExecutionEnvironment;
import com.intellij.execution.runners.ExecutionUtil;
import com.intellij.execution.ui.RunContentDescriptor;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.progress.Task;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.Messages;
import com.intellij.openapi.util.NlsContexts;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.concurrency.AsyncPromise;
import org.jetbrains.concurrency.Promise;
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

    protected RsAsyncRunner(@NotNull String executorId,
                            @SuppressWarnings("UnstableApiUsage") @NlsContexts.DialogTitle @NotNull String errorMessageTitle) {
        myExecutorId = executorId;
        myErrorMessageTitle = errorMessageTitle;
    }

    @Override
    public boolean canRun(@NotNull String executorId, @NotNull RunProfile profile) {
        if (!executorId.equals(myExecutorId) || !(profile instanceof CargoCommandConfiguration)) return false;
        CargoCommandConfiguration config = (CargoCommandConfiguration) profile;
        if (!(config.clean() instanceof CargoCommandConfiguration.CleanConfiguration.Ok)) return false;
        return !RunConfigUtil.getHasRemoteTarget(config) &&
            !CargoBuildManager.INSTANCE.isBuildToolWindowAvailable(config) &&
            !CargoBuildManager.INSTANCE.isBuildConfiguration(config) &&
            CargoBuildManager.INSTANCE.getBuildConfiguration(config) != null;
    }

    @NotNull
    @Override
    public Promise<RunContentDescriptor> execute(@NotNull ExecutionEnvironment environment, @NotNull RunProfileState state) {
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
        return buildProjectAndGetBinaryArtifactPath(environment.getProject(), buildCommand, cargoState, isTestRun)
            .then(binary -> {
                if (finalIsTestRun && finalCmdHasNoRun) return null;
                if (binary == null) return null;
                Path path = binary.getPath();
                GeneralCommandLine runCommand = cargoState.getToolchain().createGeneralCommandLine(
                    path,
                    commandLine.getWorkingDirectory(),
                    commandLine.getRedirectInputFrom(),
                    commandLine.getBacktraceMode(),
                    commandLine.getEnvironmentVariables(),
                    executableArguments,
                    false, // emulateTerminal
                    commandLine.getWithSudo(),
                    false, // patchToRemote
                    null // http
                );
                try {
                    return getRunContentDescriptor(cargoState, environment, runCommand);
                } catch (com.intellij.execution.ExecutionException e) {
                    throw new RuntimeException(e);
                }
            });
    }

    @Nullable
    protected RunContentDescriptor getRunContentDescriptor(
        @NotNull CargoRunStateBase state,
        @NotNull ExecutionEnvironment environment,
        @NotNull GeneralCommandLine runCommand
    ) throws com.intellij.execution.ExecutionException {
        DefaultExecutionResult executionResult = executeCommandLine(state, runCommand, environment);
        return new com.intellij.execution.runners.RunContentBuilder(executionResult, environment).showRunContent(environment.getContentToReuse());
    }

    @NotNull
    private DefaultExecutionResult executeCommandLine(
        @NotNull CargoRunStateBase state,
        @NotNull GeneralCommandLine commandLine,
        @NotNull ExecutionEnvironment environment
    ) throws com.intellij.execution.ExecutionException {
        return RunConfigUtil.executeCommandLine(state, commandLine, environment);
    }

    protected boolean checkToolchainConfigured(@NotNull Project project) {
        return true;
    }

    @Nullable
    protected BuildResult.ToolchainError checkToolchainSupported(@NotNull Project project, @NotNull String host) {
        return null;
    }

    protected void processUnsupportedToolchain(
        @NotNull Project project,
        @NotNull BuildResult.ToolchainError toolchainError,
        @NotNull AsyncPromise<Binary> promise
    ) {
        showErrorDialog(project, toolchainError.getMessage());
        promise.setResult(null);
    }

    @NotNull
    private Promise<Binary> buildProjectAndGetBinaryArtifactPath(
        @NotNull Project project,
        @NotNull CargoCommandLine command,
        @NotNull CargoRunStateBase state,
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
        } catch (com.intellij.execution.ExecutionException e) {
            throw new RuntimeException(e);
        }

        processForUser.addProcessListener(new CapturingProcessAdapter(processForUserOutput));

        ApplicationManager.getApplication().invokeLater(() -> {
            if (!checkToolchainConfigured(project)) {
                promise.setResult(null);
                return;
            }

            RunContentExecutor executor = new RunContentExecutor(project, processForUser);
            for (com.intellij.execution.filters.Filter filter : RunConfigUtil.createFilters(state.getCargoProject())) {
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
                    public void run(@NotNull ProgressIndicator indicator) {
                        indicator.setIndeterminate(true);
                        org.rust.cargo.toolchain.impl.RustcVersion rustVersion = state.rustVersion();
                        String host = rustVersion != null && rustVersion.getHost() != null ? rustVersion.getHost() : "";
                        result = checkToolchainSupported(project, host);
                        if (result != null) return;

                        CargoCommandLine jsonCommand = command.prependArgument("--message-format=json");
                        RsResult<RsCapturingProcessHandler, ?> handlerResult = RsCapturingProcessHandler.startProcess(
                            ((org.rust.cargo.toolchain.tools.Cargo) cargo).toGeneralCommandLine(project, jsonCommand)
                        );
                        RsCapturingProcessHandler processForJson = (RsCapturingProcessHandler) handlerResult.unwrap();
                        processForJson.setHasPty(toolchain instanceof RsWslToolchain);
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
                    public void onThrowable(@NotNull Throwable error) {
                        promise.setResult(null);
                    }
                }.queue();
            }).run();
        });

        return promise;
    }

    protected void showErrorDialog(@NotNull Project project,
                                   @SuppressWarnings("UnstableApiUsage") @NlsContexts.DialogMessage @NotNull String message) {
        Messages.showErrorDialog(project, message, myErrorMessageTitle);
    }

    public static class Binary {
        private final Path myPath;

        public Binary(@NotNull Path path) {
            myPath = path;
        }

        @NotNull
        public Path getPath() {
            return myPath;
        }
    }
}
