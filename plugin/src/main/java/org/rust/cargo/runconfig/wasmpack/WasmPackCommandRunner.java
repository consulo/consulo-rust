/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.cargo.runconfig.wasmpack;

import consulo.process.ExecutionException;
import consulo.execution.configuration.RunProfile;
import consulo.execution.executor.DefaultRunExecutor;
import consulo.execution.runner.ExecutionEnvironment;
import jakarta.annotation.Nonnull;
import org.rust.cargo.runconfig.RsDefaultProgramRunnerBase;
import org.rust.cargo.toolchain.tools.Cargo;

public class WasmPackCommandRunner extends RsDefaultProgramRunnerBase {

    public static final String RUNNER_ID = "WasmPackRunner";

    @Nonnull
    @Override
    public String getRunnerId() {
        return RUNNER_ID;
    }

    @Override
    public boolean canRun(@Nonnull String executorId, @Nonnull RunProfile profile) {
        if (!DefaultRunExecutor.EXECUTOR_ID.equals(executorId) || !(profile instanceof WasmPackCommandConfiguration)) {
            return false;
        }
        return true;
    }

    @Override
    public void execute(@Nonnull ExecutionEnvironment environment) throws ExecutionException {
        if (Cargo.checkNeedInstallWasmPack(environment.getProject())) return;
        super.execute(environment);
    }
}
