/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.cargo.runconfig;

import consulo.annotation.component.ExtensionImpl;
import consulo.execution.configuration.RunProfile;
import consulo.execution.configuration.RunProfileState;
import consulo.execution.executor.DefaultRunExecutor;
import consulo.execution.runner.ExecutionEnvironment;
import consulo.execution.ui.RunContentDescriptor;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import org.rust.cargo.runconfig.buildtool.CargoBuildManager;
import org.rust.cargo.runconfig.command.CargoCommandConfiguration;

import java.util.List;
import consulo.process.ExecutionException;

@ExtensionImpl
public class CargoCommandRunner extends RsDefaultProgramRunnerBase {

    public static final String RUNNER_ID = "CargoCommandRunner";

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
        return isLocalRun && !isLegacyTestRun;
    }

    @Nullable
    @Override
    protected RunContentDescriptor doExecute(@Nonnull RunProfileState state, @Nonnull ExecutionEnvironment environment) throws consulo.process.ExecutionException {
        RunProfile configuration = environment.getRunProfile();
        if (configuration instanceof CargoCommandConfiguration &&
            !(CargoBuildManager.INSTANCE.isBuildConfiguration((CargoCommandConfiguration) configuration) &&
                CargoBuildManager.INSTANCE.isBuildToolWindowAvailable((CargoCommandConfiguration) configuration))) {
            return super.doExecute(state, environment);
        } else {
            // For commands like `cargo build` or `cargo test --no-run`
            // we skip execution here because build already was performed
            // in Build Tool window
            return null;
        }
    }
}
