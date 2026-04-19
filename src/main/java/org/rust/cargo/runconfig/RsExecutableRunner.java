/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.cargo.runconfig;

import com.intellij.execution.DefaultExecutionResult;
import com.intellij.execution.ExecutionException;
import com.intellij.execution.configurations.GeneralCommandLine;
import com.intellij.execution.configurations.RunProfile;
import com.intellij.execution.configurations.RunProfileState;
import com.intellij.execution.runners.ExecutionEnvironment;
import com.intellij.execution.runners.RunContentBuilder;
import com.intellij.execution.ui.RunContentDescriptor;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.Messages;
import com.intellij.openapi.util.Key;
import com.intellij.openapi.util.NlsContexts;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.rust.RsBundle;
import org.rust.cargo.project.model.CargoProjectServiceUtil;
import org.rust.cargo.project.model.CargoProject;
import org.rust.cargo.project.workspace.CargoWorkspace;
import org.rust.cargo.project.workspace.PackageOrigin;
import org.rust.cargo.runconfig.buildtool.CargoBuildManager;
import org.rust.cargo.runconfig.buildtool.Utils;
import org.rust.cargo.runconfig.command.CargoCommandConfiguration;
import org.rust.cargo.toolchain.CargoCommandLine;
import org.rust.cargo.toolchain.impl.RustcMessage.CompilerArtifactMessage;
import org.rust.cargo.toolchain.tools.Cargo;
import org.rust.cargo.util.CargoArgsParser;
import org.rust.cargo.util.ParsedCargoArgs;
import org.rust.stdext.PathUtil;

import java.nio.file.Path;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.function.Function;

public abstract class RsExecutableRunner extends RsDefaultProgramRunnerBase {

    private static final Key<CompletableFuture<List<CompilerArtifactMessage>>> ARTIFACTS =
        Key.create("CARGO.CONFIGURATION.ARTIFACTS");

    @NotNull
    private final String myExecutorId;
    @NotNull
    @NlsContexts.DialogTitle
    private final String myErrorMessageTitle;

    protected RsExecutableRunner(
        @NotNull String executorId,
        @NotNull @NlsContexts.DialogTitle String errorMessageTitle
    ) {
        myExecutorId = executorId;
        myErrorMessageTitle = errorMessageTitle;
    }

    @Override
    public boolean canRun(@NotNull String executorId, @NotNull RunProfile profile) {
        if (!myExecutorId.equals(executorId) || !(profile instanceof CargoCommandConfiguration)) return false;
        CargoCommandConfiguration config = (CargoCommandConfiguration) profile;
        if (config.clean().getOk() == null) return false;
        return !RunConfigUtil.getHasRemoteTarget(config) &&
            CargoBuildManager.INSTANCE.isBuildToolWindowAvailable(config) &&
            !CargoBuildManager.INSTANCE.isBuildConfiguration(config) &&
            CargoBuildManager.INSTANCE.getBuildConfiguration(config) != null;
    }

    @Override
    public void execute(@NotNull ExecutionEnvironment environment) throws ExecutionException {
        CargoRunStateBase state = (CargoRunStateBase) environment.getState();
        if (state == null) return;
        Project project = environment.getProject();
        String host = org.rust.openapiext.OpenApiUtil.computeWithCancelableProgress(
            project,
            RsBundle.message("progress.title.checking.if.toolchain.supported"),
            () -> {
                org.rust.cargo.toolchain.impl.RustcVersion rustcVersion = state.rustVersion();
                String h = rustcVersion != null ? rustcVersion.getHost() : null;
                return h != null ? h : "";
            }
        );
        if (!checkToolchainConfigured(project)) return;
        BuildResult.ToolchainError toolchainError = checkToolchainSupported(project, host);
        if (toolchainError != null) {
            processInvalidToolchain(project, toolchainError);
            return;
        }
        List<Function<CargoCommandLine, CargoCommandLine>> patches = new ArrayList<>(Utils.getCargoPatches(environment));
        patches.add(Cargo.getCargoCommonPatch(project));
        Utils.setCargoPatches(environment, patches);
        environment.putUserData(ARTIFACTS, new CompletableFuture<>());
        super.execute(environment);
    }

    @Nullable
    @Override
    protected RunContentDescriptor doExecute(@NotNull RunProfileState state, @NotNull ExecutionEnvironment environment) throws ExecutionException {
        if (!(state instanceof CargoRunStateBase)) return null;
        CargoRunStateBase cargoState = (CargoRunStateBase) state;

        List<CompilerArtifactMessage> artifacts = getArtifacts(environment);
        if (artifacts == null) artifacts = Collections.emptyList();
        CompilerArtifactMessage artifact = artifacts.isEmpty() ? null : artifacts.get(0);
        List<String> binaries = artifact != null ? artifact.getExecutables() : Collections.emptyList();

        String errorMessage = checkErrors(artifacts, "artifact");
        if (errorMessage == null) {
            errorMessage = checkErrors(binaries, "binary");
        }
        if (errorMessage != null) {
            showErrorDialog(environment.getProject(), errorMessage);
            return null;
        }

        String packageId = artifact != null ? artifact.getPackageId() : null;
        CargoWorkspace.Package pkg = null;
        if (packageId != null) {
            for (CargoProject cargoProject : CargoProjectServiceUtil.getCargoProjects(environment.getProject()).getAllProjects()) {
                CargoWorkspace workspace = cargoProject.getWorkspace();
                if (workspace != null) {
                    CargoWorkspace.Package found = workspace.findPackageById(packageId);
                    if (found != null && found.getOrigin() == PackageOrigin.WORKSPACE) {
                        pkg = found;
                        break;
                    }
                }
            }
        }

        CargoCommandLine runCargoCommand = cargoState.prepareCommandLine().copy(
            cargoState.prepareCommandLine().getCommand(),
            cargoState.prepareCommandLine().getWorkingDirectory(),
            cargoState.prepareCommandLine().getAdditionalArguments(),
            cargoState.prepareCommandLine().getRedirectInputFrom(),
            false, // emulateTerminal
            cargoState.prepareCommandLine().getBacktraceMode(),
            cargoState.prepareCommandLine().getToolchain(),
            cargoState.prepareCommandLine().getChannel(),
            cargoState.prepareCommandLine().getEnvironmentVariables(),
            cargoState.prepareCommandLine().getRequiredFeatures(),
            cargoState.prepareCommandLine().getAllFeatures(),
            cargoState.prepareCommandLine().getWithSudo()
        );
        Path workingDirectory;
        if (pkg != null && pkg.getRootDirectory() != null &&
            List.of("test", "bench").contains(runCargoCommand.getCommand())) {
            workingDirectory = pkg.getRootDirectory();
        } else {
            workingDirectory = runCargoCommand.getWorkingDirectory();
        }
        Map<String, String> pkgEnv = pkg != null && pkg.getEnv() != null ? pkg.getEnv() : Collections.emptyMap();
        Map<String, String> combinedEnvs = new HashMap<>(runCargoCommand.getEnvironmentVariables().getEnvs());
        combinedEnvs.putAll(pkgEnv);
        com.intellij.execution.configuration.EnvironmentVariablesData environmentVariables =
            runCargoCommand.getEnvironmentVariables().with(combinedEnvs);
        ParsedCargoArgs parsedArgs = CargoArgsParser.parseArgs(
            runCargoCommand.getCommand(),
            runCargoCommand.getAdditionalArguments()
        );
        List<String> executableArguments = new ArrayList<>(parsedArgs.executableArguments());
        if ("bench".equals(runCargoCommand.getCommand())) {
            executableArguments.add("--bench");
        }
        GeneralCommandLine runExecutable = cargoState.getToolchain().createGeneralCommandLine(
            PathUtil.toPath(binaries.get(0)),
            workingDirectory,
            runCargoCommand.getRedirectInputFrom(),
            runCargoCommand.getBacktraceMode(),
            environmentVariables,
            executableArguments,
            runCargoCommand.getEmulateTerminal(),
            runCargoCommand.getWithSudo(),
            false, // patchToRemote - patching is performed for debugger/profiler/valgrind on CLion side if needed
            null // http
        );

        return showRunContent(cargoState, environment, runExecutable);
    }

    @Nullable
    protected RunContentDescriptor showRunContent(
        @NotNull CargoRunStateBase state,
        @NotNull ExecutionEnvironment environment,
        @NotNull GeneralCommandLine runExecutable
    ) throws ExecutionException {
        DefaultExecutionResult executionResult = executeCommandLine(state, runExecutable, environment);
        return new RunContentBuilder(executionResult, environment).showRunContent(environment.getContentToReuse());
    }

    @NotNull
    private DefaultExecutionResult executeCommandLine(
        @NotNull CargoRunStateBase state,
        @NotNull GeneralCommandLine commandLine,
        @NotNull ExecutionEnvironment environment
    ) throws com.intellij.execution.ExecutionException {
        return RunConfigUtil.executeCommandLine(state, commandLine, environment);
    }

    @Nullable
    public BuildResult.ToolchainError checkToolchainSupported(@NotNull Project project, @NotNull String host) {
        return null;
    }

    public boolean checkToolchainConfigured(@NotNull Project project) {
        return true;
    }

    public void processInvalidToolchain(@NotNull Project project, @NotNull BuildResult.ToolchainError toolchainError) {
        showErrorDialog(project, toolchainError.getMessage());
    }

    private void showErrorDialog(@NotNull Project project, @NotNull @NlsContexts.DialogMessage String message) {
        Messages.showErrorDialog(project, message, myErrorMessageTitle);
    }

    @Nullable
    @SuppressWarnings("UnstableApiUsage")
    private static String checkErrors(@NotNull List<?> items, @NotNull String itemName) {
        if (items.isEmpty()) {
            return "Can't find a " + itemName + ".";
        }
        if (items.size() > 1) {
            return "More than one " + itemName + " was produced. " +
                "Please specify `--bin`, `--lib`, `--test` or `--example` flag explicitly.";
        }
        return null;
    }

    @Nullable
    public static List<CompilerArtifactMessage> getArtifacts(@NotNull ExecutionEnvironment environment) {
        CompletableFuture<List<CompilerArtifactMessage>> future = environment.getUserData(ARTIFACTS);
        if (future == null) return null;
        return future.getNow(null);
    }

    public static void setArtifacts(@NotNull ExecutionEnvironment environment, @Nullable List<CompilerArtifactMessage> value) {
        CompletableFuture<List<CompilerArtifactMessage>> future = environment.getUserData(ARTIFACTS);
        if (future != null) {
            future.complete(value);
        }
    }
}
