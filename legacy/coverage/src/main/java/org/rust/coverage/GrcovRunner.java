/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.coverage;

import com.intellij.coverage.CoverageExecutor;
import com.intellij.coverage.CoverageHelper;
import com.intellij.coverage.CoverageRunnerData;
import com.intellij.execution.ExecutionException;
import com.intellij.execution.configuration.EnvironmentVariablesData;
import com.intellij.execution.configurations.ConfigurationInfoProvider;
import com.intellij.execution.configurations.RunConfigurationBase;
import com.intellij.execution.configurations.RunProfile;
import com.intellij.execution.configurations.RunProfileState;
import com.intellij.execution.configurations.RunnerSettings;
import com.intellij.execution.configurations.coverage.CoverageEnabledConfiguration;
import com.intellij.execution.process.OSProcessHandler;
import com.intellij.execution.process.ProcessAdapter;
import com.intellij.execution.process.ProcessEvent;
import com.intellij.execution.runners.ExecutionEnvironment;
import com.intellij.execution.ui.RunContentDescriptor;
import com.intellij.openapi.application.WriteAction;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.util.Key;
import com.intellij.openapi.vfs.LocalFileSystem;
import com.intellij.openapi.vfs.VfsUtil;
import com.intellij.openapi.vfs.VirtualFile;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.rust.cargo.CargoConstants;
import org.rust.cargo.project.settings.RsProjectSettingsServiceUtil;
import org.rust.cargo.runconfig.CargoRunStateBase;
import org.rust.cargo.runconfig.RunConfigUtil;
import org.rust.cargo.runconfig.RsDefaultProgramRunnerBase;
import org.rust.cargo.runconfig.buildtool.CargoBuildManager;
import org.rust.cargo.runconfig.command.CargoCommandConfiguration;
import org.rust.cargo.toolchain.CargoCommandLine;
import org.rust.cargo.toolchain.RsToolchainBase;
import org.rust.cargo.toolchain.tools.Cargo;
import org.rust.cargo.toolchain.tools.Rustup;
import org.rust.ide.experiments.RsExperiments;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

public class GrcovRunner extends RsDefaultProgramRunnerBase {

    private static final Logger LOG = Logger.getInstance(GrcovRunner.class);

    public static final String RUNNER_ID = "GrcovRunner";

    @NotNull
    @Override
    public String getRunnerId() {
        return RUNNER_ID;
    }

    @Override
    public boolean canRun(@NotNull String executorId, @NotNull RunProfile profile) {
        if (!CoverageExecutor.EXECUTOR_ID.equals(executorId) || !(profile instanceof CargoCommandConfiguration)) {
            return false;
        }
        CargoCommandConfiguration config = (CargoCommandConfiguration) profile;
        if (!(config.clean() instanceof CargoCommandConfiguration.CleanConfiguration.Ok)) {
            return false;
        }
        return !RunConfigUtil.getHasRemoteTarget(config)
            && !CargoBuildManager.INSTANCE.isBuildConfiguration(config)
            && CargoBuildManager.INSTANCE.getBuildConfiguration(config) != null;
    }

    @Nullable
    @Override
    public RunnerSettings createConfigurationData(@NotNull ConfigurationInfoProvider settingsProvider) {
        return new CoverageRunnerData();
    }

    @Override
    public void execute(@NotNull ExecutionEnvironment environment) throws ExecutionException {
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
    protected RunContentDescriptor doExecute(@NotNull RunProfileState state, @NotNull ExecutionEnvironment environment) throws ExecutionException {
        Path workingDirectory = getWorkingDirectory(environment);
        RunContentDescriptor descriptor = super.doExecute(state, environment);
        if (descriptor != null && descriptor.getProcessHandler() != null) {
            descriptor.getProcessHandler().addProcessListener(new ProcessAdapter() {
                @Override
                public void processTerminated(@NotNull ProcessEvent event) {
                    startCollectingCoverage(workingDirectory, environment);
                }
            });
        }
        return descriptor;
    }

    @NotNull
    private static Path getWorkingDirectory(@NotNull ExecutionEnvironment environment) {
        CargoRunStateBase state;
        try {
            state = (CargoRunStateBase) environment.getState();
        } catch (com.intellij.execution.ExecutionException e) {
            throw new RuntimeException(e);
        }
        assert state != null;
        return state.getCommandLine().getWorkingDirectory();
    }

    // Variables are copied from here - https://github.com/mozilla/grcov#grcov-with-travis
    @NotNull
    private static CargoCommandLine applyCargoCoveragePatch(
        @NotNull CargoCommandLine commandLine
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

    private static void cleanOldCoverageData(@NotNull Path workingDirectory) {
        VirtualFile root = LocalFileSystem.getInstance().refreshAndFindFileByIoFile(workingDirectory.toFile());
        if (root == null) return;
        VirtualFile targetDir = root.findChild(CargoConstants.ProjectLayout.target);
        if (targetDir == null) return;

        List<VirtualFile> toDelete = new ArrayList<>();
        VfsUtil.iterateChildrenRecursively(targetDir, null, fileOrDir -> {
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

    private static void startCollectingCoverage(@NotNull Path workingDirectory, @NotNull ExecutionEnvironment environment) {
        var project = environment.getProject();
        RunConfigurationBase<?> runConfiguration = environment.getRunProfile() instanceof RunConfigurationBase<?>
            ? (RunConfigurationBase<?>) environment.getRunProfile() : null;
        if (runConfiguration == null) return;
        RunnerSettings runnerSettings = environment.getRunnerSettings();
        if (runnerSettings == null) return;
        RsToolchainBase toolchain = RsProjectSettingsServiceUtil.getToolchain(project);
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
            OSProcessHandler coverageProcess = new OSProcessHandler(coverageCmd);
            rsCoverageConfig.coverageProcess = coverageProcess;
            CoverageHelper.attachToProcess(runConfiguration, coverageProcess, runnerSettings);
            coverageProcess.addProcessListener(new ProcessAdapter() {
                @Override
                public void onTextAvailable(@NotNull ProcessEvent event, @NotNull Key outputType) {
                    LOG.debug(event.getText());
                }
            });
            coverageProcess.startNotify();
        } catch (ExecutionException e) {
            LOG.error(e);
        }
    }
}
