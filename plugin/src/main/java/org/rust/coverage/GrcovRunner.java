/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.coverage;

import consulo.execution.coverage.CoverageExecutor;
import consulo.execution.coverage.CoverageHelper;
import consulo.execution.coverage.CoverageRunnerData;
import consulo.process.ExecutionException;
import consulo.execution.configuration.EnvironmentVariablesData;
import consulo.execution.configuration.ConfigurationInfoProvider;
import consulo.execution.configuration.RunConfigurationBase;
import consulo.execution.configuration.RunProfile;
import consulo.execution.configuration.RunProfileState;
import consulo.execution.configuration.RunnerSettings;
import consulo.execution.coverage.CoverageEnabledConfiguration;
import consulo.process.ProcessHandler;
import consulo.process.local.ProcessHandlerFactory;
import consulo.process.event.ProcessAdapter;
import consulo.process.event.ProcessEvent;
import consulo.execution.runner.ExecutionEnvironment;
import consulo.execution.ui.RunContentDescriptor;
import consulo.application.WriteAction;
import consulo.logging.Logger;
import consulo.util.dataholder.Key;
import consulo.virtualFileSystem.LocalFileSystem;
import consulo.virtualFileSystem.util.VirtualFileUtil;
import consulo.virtualFileSystem.VirtualFile;
import consulo.annotation.component.ExtensionImpl;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import org.rust.cargo.CargoConstants;
import org.rust.cargo.runconfig.CargoRunStateBase;
import org.rust.cargo.runconfig.RunConfigUtil;
import org.rust.cargo.runconfig.RsDefaultProgramRunnerBase;
import org.rust.cargo.runconfig.command.CargoCommandConfiguration;
import org.rust.cargo.toolchain.CargoCommandLine;
import org.rust.cargo.toolchain.RsToolchainBase;
import org.rust.cargo.toolchain.tools.Cargo;
import org.rust.cargo.toolchain.tools.Rustup;
import org.rust.experiments.RsExperiments;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

@ExtensionImpl
public class GrcovRunner extends RsDefaultProgramRunnerBase {

    private static final Logger LOG = Logger.getInstance(GrcovRunner.class);

    public static final String RUNNER_ID = "GrcovRunner";

    @Nonnull
    @Override
    public String getRunnerId() {
        return RUNNER_ID;
    }

    @Override
    public boolean canRun(@Nonnull String executorId, @Nonnull RunProfile profile) {
        if (!CoverageExecutor.EXECUTOR_ID.equals(executorId) || !(profile instanceof CargoCommandConfiguration)) {
            return false;
        }
        CargoCommandConfiguration config = (CargoCommandConfiguration) profile;
        if (!(config.clean() instanceof CargoCommandConfiguration.CleanConfiguration.Ok)) {
            return false;
        }
        // grcov instruments the binary a run or test command produces; a plain `cargo build`
        // configuration runs nothing, so there is no coverage to collect from it.
        String command = config.getCommand();
        return !RunConfigUtil.getHasRemoteTarget(config)
            && command != null
            && !command.trim().startsWith("build");
    }

    @Nullable
    @Override
    public RunnerSettings createConfigurationData(@Nonnull ConfigurationInfoProvider settingsProvider) {
        return new CoverageRunnerData();
    }

    @Override
    public void execute(@Nonnull ExecutionEnvironment environment) throws ExecutionException {
        if (Cargo.checkNeedInstallGrcov(environment.getProject())) return;
        Path workingDirectory = getWorkingDirectory(environment);
        if (org.rust.openapiext.OpenApiUtil.isFeatureEnabled(RsExperiments.SOURCE_BASED_COVERAGE)) {
            if (Rustup.checkNeedInstallLlvmTools(environment.getProject(), workingDirectory)) return;
        } else {
            cleanOldCoverageData(workingDirectory);
        }
        List<Function<CargoCommandLine, CargoCommandLine>> patches =
            new ArrayList<>(org.rust.cargo.runconfig.buildtool.Utils.getCargoPatches(environment));
        patches.add(GrcovRunner::applyCargoCoveragePatch);
        org.rust.cargo.runconfig.buildtool.Utils.setCargoPatches(environment, patches);
        super.execute(environment);
    }

    @Nullable
    @Override
    protected RunContentDescriptor doExecute(@Nonnull RunProfileState state, @Nonnull ExecutionEnvironment environment) throws ExecutionException {
        RunContentDescriptor descriptor = super.doExecute(state, environment);
        if (!(state instanceof CargoRunStateBase cargoState)) return descriptor;
        // The toolchain comes from the run state, which resolves it from the module's Rust extension.
        Path workingDirectory = cargoState.getCommandLine().getWorkingDirectory();
        RsToolchainBase toolchain = cargoState.getToolchain();
        if (descriptor != null && descriptor.getProcessHandler() != null) {
            descriptor.getProcessHandler().addProcessListener(new ProcessAdapter() {
                @Override
                public void processTerminated(@Nonnull ProcessEvent event) {
                    startCollectingCoverage(workingDirectory, toolchain, environment);
                }
            });
        }
        return descriptor;
    }

    @Nonnull
    private static Path getWorkingDirectory(@Nonnull ExecutionEnvironment environment) {
        CargoRunStateBase state;
        try {
            state = (CargoRunStateBase) environment.getState();
        } catch (ExecutionException e) {
            throw new RuntimeException(e);
        }
        assert state != null;
        return state.getCommandLine().getWorkingDirectory();
    }

    // Variables are copied from here - https://github.com/mozilla/grcov#grcov-with-travis
    @Nonnull
    private static CargoCommandLine applyCargoCoveragePatch(
        @Nonnull CargoCommandLine commandLine
    ) {
        String rustcFlags;
        if (org.rust.openapiext.OpenApiUtil.isFeatureEnabled(RsExperiments.SOURCE_BASED_COVERAGE)) {
            rustcFlags = "-Cinstrument-coverage";
        } else {
            rustcFlags = "-Zprofile -Ccodegen-units=1 -Copt-level=0 -Clink-dead-code -Coverflow-checks=off";
        }
        EnvironmentVariablesData oldVariables = commandLine.getEnvironmentVariables();
        Map<String, String> newEnvs = new HashMap<>(oldVariables.getEnvs());
        newEnvs.put(RsToolchainBase.RUSTC_BOOTSTRAP, "1");
        newEnvs.put("CARGO_INCREMENTAL", "0");
        newEnvs.put("RUSTFLAGS", rustcFlags);
        newEnvs.put("LLVM_PROFILE_FILE", "grcov-%p-%m.profraw");
        EnvironmentVariablesData environmentVariables = EnvironmentVariablesData.create(
            newEnvs,
            oldVariables.isPassParentEnvs()
        );
        return commandLine.copy(
            commandLine.getCommand(),
            commandLine.getWorkingDirectory(),
            commandLine.getAdditionalArguments(),
            commandLine.getRedirectInputFrom(),
            commandLine.getEmulateTerminal(),
            commandLine.getBacktraceMode(),
            commandLine.getToolchain(),
            commandLine.getChannel(),
            environmentVariables,
            commandLine.getRequiredFeatures(),
            commandLine.getAllFeatures(),
            commandLine.getWithSudo()
        );
    }

    private static void cleanOldCoverageData(@Nonnull Path workingDirectory) {
        VirtualFile root = LocalFileSystem.getInstance().refreshAndFindFileByIoFile(workingDirectory.toFile());
        if (root == null) return;
        VirtualFile targetDir = root.findChild(CargoConstants.ProjectLayout.target);
        if (targetDir == null) return;

        List<VirtualFile> toDelete = new ArrayList<>();
        VirtualFileUtil.iterateChildrenRecursively(targetDir, null, fileOrDir -> {
            if (!fileOrDir.isDirectory() && "gcda".equals(fileOrDir.getExtension())) {
                toDelete.add(fileOrDir);
            }
            return true;
        });

        if (toDelete.isEmpty()) return;
        WriteAction.runAndWait(() -> {
            for (VirtualFile file : toDelete) {
                try {
                    file.delete(null);
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
            }
        });
    }

    private static void startCollectingCoverage(@Nonnull Path workingDirectory,
                                               @Nullable RsToolchainBase toolchain,
                                               @Nonnull ExecutionEnvironment environment) {
        var project = environment.getProject();
        RunConfigurationBase runConfiguration = environment.getRunProfile() instanceof RunConfigurationBase
            ? (RunConfigurationBase) environment.getRunProfile() : null;
        if (runConfiguration == null) return;
        RunnerSettings runnerSettings = environment.getRunnerSettings();
        if (runnerSettings == null) return;
        if (toolchain == null) return;
        Grcov grcov = Grcov.grcov(toolchain);
        if (grcov == null) return;

        CoverageEnabledConfiguration coverageEnabledConfiguration = CoverageEnabledConfiguration.getOrCreate(runConfiguration);
        if (!(coverageEnabledConfiguration instanceof RsCoverageEnabledConfiguration)) return;
        RsCoverageEnabledConfiguration rsCoverageConfig = (RsCoverageEnabledConfiguration) coverageEnabledConfiguration;
        String coverageFilePathStr = rsCoverageConfig.getCoverageFilePath();
        if (coverageFilePathStr == null) return;
        Path coverageFilePath = Path.of(coverageFilePathStr);
        var coverageCmd = grcov.createCommandLine(workingDirectory, coverageFilePath);

        try {
            ProcessHandler coverageProcess = ProcessHandlerFactory.getInstance().createProcessHandler(coverageCmd);
            rsCoverageConfig.coverageProcess = coverageProcess;
            CoverageHelper.attachToProcess(runConfiguration, coverageProcess, runnerSettings);
            coverageProcess.addProcessListener(new ProcessAdapter() {
                @Override
                public void onTextAvailable(@Nonnull ProcessEvent event, @Nonnull Key outputType) {
                    LOG.debug(event.getText());
                }
            });
            coverageProcess.startNotify();
        } catch (ExecutionException e) {
            LOG.error(e);
        }
    }
}
