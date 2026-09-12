/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.cargo.runconfig;

import consulo.annotation.component.ExtensionImpl;
import consulo.execution.configuration.RunProfile;
import consulo.execution.executor.DefaultRunExecutor;
import jakarta.annotation.Nonnull;
import org.rust.cargo.runconfig.command.CargoCommandConfiguration;


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
        return !RunConfigUtil.getHasRemoteTarget(config) || config.getBuildTarget().isRemote();
    }
}
