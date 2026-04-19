/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.cargo.runconfig.buildtool;

import com.intellij.execution.configurations.RunConfiguration;
import com.intellij.execution.runners.ExecutionEnvironment;
import com.intellij.openapi.actionSystem.DataContext;
import com.intellij.openapi.util.Key;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.util.execution.ParametersListUtil;
import org.rust.cargo.runconfig.command.CargoCommandConfiguration;
import org.rust.cargo.toolchain.tools.Rustup;

import java.util.List;

public class CargoBuildTaskProvider extends RsBuildTaskProvider<CargoBuildTaskProvider.BuildTask> {

    @SuppressWarnings("rawtypes")
    public static final Key<BuildTask> ID = Key.create("CARGO.BUILD_TASK_PROVIDER");

    @Override
    public Key<BuildTask> getId() {
        return ID;
    }

    @Override
    public BuildTask createTask(RunConfiguration runConfiguration) {
        if (runConfiguration instanceof CargoCommandConfiguration) {
            return new BuildTask();
        }
        return null;
    }

    @Override
    public boolean executeTask(
        DataContext context,
        RunConfiguration configuration,
        ExecutionEnvironment environment,
        BuildTask task
    ) {
        if (!(configuration instanceof CargoCommandConfiguration cargoConfig)) return false;
        CargoCommandConfiguration buildConfiguration = CargoBuildManager.INSTANCE.getBuildConfiguration(cargoConfig);
        if (buildConfiguration == null) return true;

        java.nio.file.Path projectDirectory = buildConfiguration.getWorkingDirectory();
        if (projectDirectory == null) return false;

        List<String> configArgs = ParametersListUtil.parse(buildConfiguration.getCommand());
        int targetFlagIdx = -1;
        for (int i = 0; i < configArgs.size(); i++) {
            if (configArgs.get(i).startsWith("--target")) {
                targetFlagIdx = i;
                break;
            }
        }
        String targetFlag = targetFlagIdx != -1 ? configArgs.get(targetFlagIdx) : null;
        String targetTriple;
        if ("--target".equals(targetFlag)) {
            targetTriple = targetFlagIdx + 1 < configArgs.size() ? configArgs.get(targetFlagIdx + 1) : null;
        } else if (targetFlag != null && targetFlag.startsWith("--target=")) {
            targetTriple = StringUtil.unquoteString(targetFlag.substring("--target=".length()));
        } else {
            targetTriple = null;
        }

        if (targetTriple != null && Rustup.checkNeedInstallTarget(configuration.getProject(), projectDirectory, targetTriple)) {
            return false;
        }

        return doExecuteTask(buildConfiguration, environment);
    }

    public static class BuildTask extends RsBuildTaskProvider.BuildTask<BuildTask> {
        public BuildTask() {
            super(ID);
        }
    }
}
