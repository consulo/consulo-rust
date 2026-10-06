/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.cargo.runconfig;

import consulo.execution.configuration.EnvironmentVariablesData;
import consulo.execution.configuration.RunProfile;
import consulo.execution.configuration.RunProfileState;
import consulo.execution.runner.ExecutionEnvironment;
import consulo.execution.ui.RunContentDescriptor;
import consulo.http.HttpProxyManager;
import consulo.process.ExecutionException;
import consulo.process.cmd.GeneralCommandLine;
import consulo.project.Project;
import consulo.rust.compiler.CargoCompilerRunner;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import org.rust.cargo.api.model.CargoProject;
import org.rust.cargo.api.model.CargoProjectsService;
import org.rust.cargo.api.toolchain.RustcMessage.CompilerArtifactMessage;
import org.rust.cargo.api.workspace.CargoWorkspace;
import org.rust.cargo.api.workspace.PackageOrigin;
import org.rust.cargo.runconfig.command.CargoCommandConfiguration;
import org.rust.cargo.toolchain.CargoCommandLine;
import org.rust.cargo.util.CargoArgsParser;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public abstract class RsExecutableRunner extends RsDefaultProgramRunnerBase {
    private static final List<String> COMMANDS = List.of("run", "test", "bench");
    private static final List<String> TEST_COMMANDS = List.of("test", "bench");

    @Override
    public boolean canRun(@Nonnull String executorId, @Nonnull RunProfile profile) {
        if (!(profile instanceof CargoCommandConfiguration config)) {
            return false;
        }
        CargoCommandConfiguration.CleanConfiguration.Ok cleaned = config.clean().getOk();
        if (cleaned == null) {
            return false;
        }
        return !RunConfigUtil.getHasRemoteTarget(config) && COMMANDS.contains(cleaned.getCmd().getCommand());
    }

    @Nullable
    @Override
    protected RunContentDescriptor doExecute(@Nonnull RunProfileState state, @Nonnull ExecutionEnvironment environment) throws ExecutionException {
        if (!(state instanceof CargoRunStateBase cargoState)) {
            return null;
        }

        CargoCommandLine runCargoCommand = cargoState.prepareCommandLine();
        String command = runCargoCommand.getCommand();
        boolean test = TEST_COMMANDS.contains(command);

        List<CompilerArtifactMessage> artifacts = new ArrayList<>();
        List<CompilerArtifactMessage> built = environment.getUserData(CargoCompilerRunner.ARTIFACTS);
        if (built != null) {
            for (CompilerArtifactMessage artifact : built) {
                if (artifact.getExecutable() != null && artifact.getProfile().isTest() == test) {
                    artifacts.add(artifact);
                }
            }
        }

        String errorMessage = checkErrors(artifacts, "binary");
        if (errorMessage != null) {
            throw new ExecutionException(errorMessage);
        }
        CompilerArtifactMessage artifact = artifacts.get(0);
        String binary = artifact.getExecutable();

        CargoWorkspace.Package pkg = findWorkspacePackage(environment.getProject(), artifact.getPackageId());

        Path workingDirectory = pkg != null && test ? pkg.getRootDirectory() : runCargoCommand.getWorkingDirectory();

        EnvironmentVariablesData environmentVariables = runCargoCommand.getEnvironmentVariables();
        if (pkg != null && !pkg.getEnv().isEmpty()) {
            Map<String, String> envs = new LinkedHashMap<>(environmentVariables.getEnvs());
            envs.putAll(pkg.getEnv());
            environmentVariables = environmentVariables.with(envs);
        }

        List<String> executableArguments = new ArrayList<>(CargoArgsParser.parseArgs(command, runCargoCommand.getAdditionalArguments()).executableArguments());
        if ("bench".equals(command)) {
            executableArguments.add("--bench");
        }

        GeneralCommandLine runExecutable = cargoState.getToolchain().createGeneralCommandLine(
            Path.of(binary),
            workingDirectory,
            runCargoCommand.getRedirectInputFrom(),
            runCargoCommand.getBacktraceMode(),
            environmentVariables,
            executableArguments,
            false,
            runCargoCommand.getWithSudo(),
            false,
            HttpProxyManager.getInstance()
        );

        return showRunContent(cargoState, environment, runExecutable);
    }

    @Nullable
    protected abstract RunContentDescriptor showRunContent(@Nonnull CargoRunStateBase state,
                                                           @Nonnull ExecutionEnvironment environment,
                                                           @Nonnull GeneralCommandLine runExecutable) throws ExecutionException;

    @Nullable
    private static String checkErrors(List<?> items, String itemName) {
        if (items.isEmpty()) {
            return "Can't find a " + itemName + ".";
        }
        if (items.size() > 1) {
            return "More than one " + itemName + " was produced. Please specify `--bin`, `--lib`, `--test` or `--example` flag explicitly.";
        }
        return null;
    }

    @Nullable
    private static CargoWorkspace.Package findWorkspacePackage(Project project, @Nullable String packageId) {
        if (packageId == null) {
            return null;
        }
        for (CargoProject cargoProject : CargoProjectsService.getInstance(project).getAllProjects()) {
            CargoWorkspace workspace = cargoProject.getWorkspace();
            if (workspace == null) {
                continue;
            }
            CargoWorkspace.Package pkg = workspace.findPackageById(packageId);
            if (pkg != null && pkg.getOrigin() == PackageOrigin.WORKSPACE) {
                return pkg;
            }
        }
        return null;
    }
}
