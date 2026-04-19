/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.cargo.runconfig.wasmpack;

import com.intellij.execution.ExecutionException;
import com.intellij.execution.configurations.RunProfile;
import com.intellij.execution.executors.DefaultRunExecutor;
import com.intellij.execution.runners.ExecutionEnvironment;
import org.jetbrains.annotations.NotNull;
import org.rust.cargo.runconfig.RsDefaultProgramRunnerBase;
import org.rust.cargo.toolchain.tools.Cargo;

public class WasmPackCommandRunner extends RsDefaultProgramRunnerBase {

    public static final String RUNNER_ID = "WasmPackRunner";

    @NotNull
    @Override
    public String getRunnerId() {
        return RUNNER_ID;
    }

    @Override
    public boolean canRun(@NotNull String executorId, @NotNull RunProfile profile) {
        if (!DefaultRunExecutor.EXECUTOR_ID.equals(executorId) || !(profile instanceof WasmPackCommandConfiguration)) {
            return false;
        }
        return true;
    }

    @Override
    public void execute(@NotNull ExecutionEnvironment environment) throws ExecutionException {
        if (Cargo.checkNeedInstallWasmPack(environment.getProject())) return;
        super.execute(environment);
    }
}
