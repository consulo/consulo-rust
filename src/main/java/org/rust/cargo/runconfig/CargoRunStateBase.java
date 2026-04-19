/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.cargo.runconfig;

import com.intellij.execution.configurations.CommandLineState;
import com.intellij.execution.configurations.GeneralCommandLine;
import com.intellij.execution.process.ProcessHandler;
import com.intellij.execution.process.ProcessTerminatedListener;
import com.intellij.execution.runners.ExecutionEnvironment;
import com.intellij.execution.target.TargetEnvironmentConfiguration;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.text.StringUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.rust.cargo.project.model.CargoProject;
import org.rust.cargo.runconfig.buildtool.Utils;
import org.rust.cargo.runconfig.command.CargoCommandConfiguration;
import org.rust.cargo.runconfig.target.RsLanguageRuntimeConfiguration;
import org.rust.cargo.toolchain.CargoCommandLine;
import org.rust.cargo.toolchain.RsToolchainBase;
import org.rust.cargo.toolchain.impl.RustcVersion;
import org.rust.cargo.toolchain.tools.Cargo;
import org.rust.cargo.toolchain.tools.CargoExtUtil;
import org.rust.cargo.toolchain.tools.Rustc;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;

public abstract class CargoRunStateBase extends CommandLineState {

    private static final Logger LOG = Logger.getInstance(CargoRunStateBase.class);
    private static final String SSH_TARGET_TYPE_ID = "ssh/sftp";

    @NotNull
    private final CargoCommandConfiguration myRunConfiguration;
    @NotNull
    private final CargoCommandConfiguration.CleanConfiguration.Ok myConfig;
    @NotNull
    private final Project myProject;
    @NotNull
    private final RsToolchainBase myToolchain;
    @NotNull
    private final CargoCommandLine myCommandLine;
    @Nullable
    private final CargoProject myCargoProject;

    protected final List<Function<CargoCommandLine, CargoCommandLine>> myCommandLinePatches;

    protected CargoRunStateBase(
        @NotNull ExecutionEnvironment environment,
        @NotNull CargoCommandConfiguration runConfiguration,
        @NotNull CargoCommandConfiguration.CleanConfiguration.Ok config
    ) {
        super(environment);
        myRunConfiguration = runConfiguration;
        myConfig = config;
        myProject = environment.getProject();
        myToolchain = config.getToolchain();
        myCommandLine = config.getCmd();
        myCargoProject = CargoCommandConfiguration.findCargoProject(
            myProject,
            myCommandLine.getAdditionalArguments(),
            myCommandLine.getWorkingDirectory()
        );
        myCommandLinePatches = new ArrayList<>(Utils.getCargoPatches(environment));
    }

    @NotNull
    public CargoCommandConfiguration getRunConfiguration() {
        return myRunConfiguration;
    }

    @NotNull
    public CargoCommandConfiguration.CleanConfiguration.Ok getConfig() {
        return myConfig;
    }

    @NotNull
    public Project getProject() {
        return myProject;
    }

    @NotNull
    public RsToolchainBase getToolchain() {
        return myToolchain;
    }

    @NotNull
    public CargoCommandLine getCommandLine() {
        return myCommandLine;
    }

    @Nullable
    public CargoProject getCargoProject() {
        return myCargoProject;
    }

    @Nullable
    private Path getWorkingDirectory() {
        return myCargoProject != null ? CargoCommandConfiguration.getWorkingDirectory(myCargoProject) : null;
    }

    @NotNull
    public Cargo cargo() {
        return Cargo.cargoOrWrapper(myToolchain, getWorkingDirectory());
    }

    @Nullable
    public RustcVersion rustVersion() {
        return Rustc.create(myToolchain).queryVersion(getWorkingDirectory());
    }

    @NotNull
    public CargoCommandLine prepareCommandLine() {
        CargoCommandLine commandLine = myCommandLine;
        for (Function<CargoCommandLine, CargoCommandLine> patch : myCommandLinePatches) {
            commandLine = patch.apply(commandLine);
        }
        return commandLine;
    }

    @SafeVarargs
    @NotNull
    public final CargoCommandLine prepareCommandLine(@NotNull Function<CargoCommandLine, CargoCommandLine>... additionalPatches) {
        CargoCommandLine commandLine = myCommandLine;
        for (Function<CargoCommandLine, CargoCommandLine> patch : myCommandLinePatches) {
            commandLine = patch.apply(commandLine);
        }
        for (Function<CargoCommandLine, CargoCommandLine> patch : additionalPatches) {
            commandLine = patch.apply(commandLine);
        }
        return commandLine;
    }

    @NotNull
    @Override
    protected ProcessHandler startProcess() throws com.intellij.execution.ExecutionException {
        return startProcess(true);
    }

    /**
     * @param processColors if true, process ANSI escape sequences, otherwise keep escape codes in the output
     */
    @NotNull
    public ProcessHandler startProcess(boolean processColors) throws com.intellij.execution.ExecutionException {
        TargetEnvironmentConfiguration targetEnvironment =
            org.rust.cargo.runconfig.target.TargetUtil.getTargetEnvironment(myRunConfiguration);
        // Fallback to non-target implementation in case of local target
        if (targetEnvironment == null) {
            GeneralCommandLine commandLine = cargo().toColoredCommandLine(getEnvironment().getProject(), prepareCommandLine());
            LOG.debug("Executing Cargo command: `" + commandLine.getCommandLineString() + "`");
            RsProcessHandler handler = new RsProcessHandler(commandLine, processColors);
            ProcessTerminatedListener.attach(handler); // shows exit code upon termination
            return handler;
        }

        TargetEnvironmentConfiguration finalTargetEnvironment = targetEnvironment;
        Function<CargoCommandLine, CargoCommandLine> remoteRunPatch = cmdLine -> {
            CargoCommandLine result;
            if (myRunConfiguration.getBuildTarget().isRemote() &&
                SSH_TARGET_TYPE_ID.equals(finalTargetEnvironment.getTypeId())) {
                result = cmdLine.prependArgument(
                    "--target-dir=" + finalTargetEnvironment.getProjectRootOnTarget() + "/target"
                );
            } else {
                result = cmdLine;
            }
            return result.copy(
                result.getCommand(),
                result.getWorkingDirectory(),
                result.getAdditionalArguments(),
                result.getRedirectInputFrom(),
                false, // emulateTerminal
                result.getBacktraceMode(),
                result.getToolchain(),
                result.getChannel(),
                result.getEnvironmentVariables(),
                result.getRequiredFeatures(),
                result.getAllFeatures(),
                result.getWithSudo()
            );
        };

        GeneralCommandLine commandLine = cargo().toColoredCommandLine(myProject, prepareCommandLine(remoteRunPatch));
        RsLanguageRuntimeConfiguration languageRuntime =
            org.rust.cargo.runconfig.target.TargetUtil.getLanguageRuntime(targetEnvironment);
        String cargoPath = languageRuntime != null ? languageRuntime.getCargoPath() : null;
        String nullizedCargoPath = StringUtil.nullize(cargoPath, true);
        commandLine.setExePath(nullizedCargoPath != null ? nullizedCargoPath : "cargo");
        return org.rust.cargo.runconfig.target.TargetUtil.startProcess(commandLine, myProject, targetEnvironment, processColors, false);
    }
}
