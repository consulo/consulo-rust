/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.cargo.runconfig;

import com.intellij.execution.configurations.RunProfile;
import com.intellij.execution.configurations.RunProfileState;
import com.intellij.execution.executors.DefaultRunExecutor;
import com.intellij.execution.impl.ExecutionManagerImpl;
import com.intellij.execution.runners.ExecutionEnvironment;
import com.intellij.execution.ui.RunContentDescriptor;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.rust.cargo.runconfig.buildtool.CargoBuildManager;
import org.rust.cargo.runconfig.command.CargoCommandConfiguration;

import java.util.List;

public class CargoCommandRunner extends RsDefaultProgramRunnerBase {

    public static final String RUNNER_ID = "CargoCommandRunner";

    @NotNull
    @Override
    public String getRunnerId() {
        return RUNNER_ID;
    }

    @Override
    public boolean canRun(@NotNull String executorId, @NotNull RunProfile profile) {
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
    protected RunContentDescriptor doExecute(@NotNull RunProfileState state, @NotNull ExecutionEnvironment environment) throws com.intellij.execution.ExecutionException {
        RunProfile configuration = environment.getRunProfile();
        if (configuration instanceof CargoCommandConfiguration &&
            !(CargoBuildManager.INSTANCE.isBuildConfiguration((CargoCommandConfiguration) configuration) &&
                CargoBuildManager.INSTANCE.isBuildToolWindowAvailable((CargoCommandConfiguration) configuration))) {
            return super.doExecute(state, environment);
        } else {
            // For commands like `cargo build` or `cargo test --no-run`
            // we skip execution here because build already was performed
            // in Build Tool window
            environment.putUserData(ExecutionManagerImpl.EXECUTION_SKIP_RUN, true);
            return null;
        }
    }
}
