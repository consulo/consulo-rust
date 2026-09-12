/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.cargo.runconfig;

import consulo.execution.configuration.CommandLineState;
import consulo.process.cmd.GeneralCommandLine;
import consulo.process.ProcessHandler;
import consulo.execution.process.ProcessTerminatedListener;
import consulo.execution.runner.ExecutionEnvironment;
import com.intellij.execution.target.TargetEnvironmentConfiguration;
import consulo.logging.Logger;
import consulo.project.Project;
import consulo.util.lang.StringUtil;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
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
import consulo.process.ExecutionException;

public abstract class CargoRunStateBase extends CommandLineState {

    private static final Logger LOG = Logger.getInstance(CargoRunStateBase.class);
    private static final String SSH_TARGET_TYPE_ID = "ssh/sftp";

    @Nonnull
    private final CargoCommandConfiguration myRunConfiguration;
    @Nonnull
    private final CargoCommandConfiguration.CleanConfiguration.Ok myConfig;
    @Nonnull
    private final Project myProject;
    @Nonnull
    private final RsToolchainBase myToolchain;
    @Nonnull
    private final CargoCommandLine myCommandLine;
    @Nullable
    private final CargoProject myCargoProject;

    protected final List<Function<CargoCommandLine, CargoCommandLine>> myCommandLinePatches;

    protected CargoRunStateBase(
        @Nonnull ExecutionEnvironment environment,
        @Nonnull CargoCommandConfiguration runConfiguration,
        @Nonnull CargoCommandConfiguration.CleanConfiguration.Ok config
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

    @Nonnull
    public CargoCommandConfiguration getRunConfiguration() {
        return myRunConfiguration;
    }

    @Nonnull
    public CargoCommandConfiguration.CleanConfiguration.Ok getConfig() {
        return myConfig;
    }

    @Nonnull
    public Project getProject() {
        return myProject;
    }

    @Nonnull
    public RsToolchainBase getToolchain() {
        return myToolchain;
    }

    @Nonnull
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

    @Nonnull
    public Cargo cargo() {
        return Cargo.cargoOrWrapper(myToolchain, getWorkingDirectory());
    }

    @Nullable
    public RustcVersion rustVersion() {
        return Rustc.create(myToolchain).queryVersion(getWorkingDirectory());
    }

    @Nonnull
    public CargoCommandLine prepareCommandLine() {
        CargoCommandLine commandLine = myCommandLine;
        for (Function<CargoCommandLine, CargoCommandLine> patch : myCommandLinePatches) {
            commandLine = patch.apply(commandLine);
        }
        return commandLine;
    }

    @SafeVarargs
    @Nonnull
    public final CargoCommandLine prepareCommandLine(@Nonnull Function<CargoCommandLine, CargoCommandLine>... additionalPatches) {
        CargoCommandLine commandLine = myCommandLine;
        for (Function<CargoCommandLine, CargoCommandLine> patch : myCommandLinePatches) {
            commandLine = patch.apply(commandLine);
        }
        for (Function<CargoCommandLine, CargoCommandLine> patch : additionalPatches) {
            commandLine = patch.apply(commandLine);
        }
        return commandLine;
    }

    @Nonnull
    @Override
    protected ProcessHandler startProcess() throws consulo.process.ExecutionException {
        return startProcess(true);
    }

    /**
     * @param processColors if true, process ANSI escape sequences, otherwise keep escape codes in the output
     */
    @Nonnull
    public ProcessHandler startProcess(boolean processColors) throws consulo.process.ExecutionException {
        // Always run against the local toolchain.
        GeneralCommandLine commandLine = cargo().toColoredCommandLine(getEnvironment().getProject(), prepareCommandLine());
        LOG.debug("Executing Cargo command: `" + commandLine.getCommandLineString() + "`");
        RsProcessHandler handler = new RsProcessHandler(commandLine, processColors);
        ProcessTerminatedListener.attach(handler); // shows exit code upon termination
        return handler;
    }
}
