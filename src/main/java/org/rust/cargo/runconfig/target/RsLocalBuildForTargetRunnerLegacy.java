/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.cargo.runconfig.target;

import com.intellij.execution.configurations.RunProfile;
import com.intellij.execution.executors.DefaultRunExecutor;
import org.jetbrains.annotations.NotNull;
import org.rust.RsBundle;
import org.rust.cargo.runconfig.buildtool.CargoBuildManager;
import org.rust.cargo.runconfig.command.CargoCommandConfiguration;
import org.rust.cargo.runconfig.legacy.RsAsyncRunner;

/**
 * This runner is used for remote targets if {@link CargoBuildManager#isBuildToolWindowAvailable} is false.
 */
public class RsLocalBuildForTargetRunnerLegacy extends RsAsyncRunner {

    public static final String RUNNER_ID = "RsLocalBuildForTargetRunnerLegacy";

    public RsLocalBuildForTargetRunnerLegacy() {
        super(DefaultRunExecutor.EXECUTOR_ID, RsBundle.message("dialog.title.unable.to.build"));
    }

    @NotNull
    @Override
    public String getRunnerId() {
        return RUNNER_ID;
    }

    @Override
    public boolean canRun(@NotNull String executorId, @NotNull RunProfile profile) {
        if (!executorId.equals(DefaultRunExecutor.EXECUTOR_ID) ||
            !(profile instanceof CargoCommandConfiguration) ||
            !(((CargoCommandConfiguration) profile).clean() instanceof CargoCommandConfiguration.CleanConfiguration.Ok)) {
            return false;
        }
        CargoCommandConfiguration config = (CargoCommandConfiguration) profile;
        return org.rust.cargo.runconfig.RunConfigUtil.getHasRemoteTarget(config) &&
            config.getBuildTarget().isLocal() &&
            !CargoBuildManager.INSTANCE.isBuildToolWindowAvailable(config);
    }
}
