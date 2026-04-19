/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.cargo.runconfig.target;

import com.intellij.execution.configurations.GeneralCommandLine;
import com.intellij.execution.process.ProcessHandler;
import com.intellij.execution.target.TargetEnvironmentConfiguration;
import com.intellij.openapi.project.Project;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.rust.cargo.runconfig.command.CargoCommandConfiguration;

import java.util.List;

/**
 * Bridge class delegating to {@link TargetUtil}.
 */
public final class UtilsUtil {

    private UtilsUtil() {
    }

    @Nullable
    public static TargetEnvironmentConfiguration getTargetEnvironment(@NotNull CargoCommandConfiguration config) {
        return TargetUtil.getTargetEnvironment(config);
    }

    @NotNull
    public static List<String> getLocalBuildArgsForRemoteRun(@NotNull CargoCommandConfiguration config) {
        return TargetUtil.getLocalBuildArgsForRemoteRun(config);
    }

    @Nullable
    public static RsLanguageRuntimeConfiguration getLanguageRuntime(@NotNull TargetEnvironmentConfiguration config) {
        return TargetUtil.getLanguageRuntime(config);
    }

    @NotNull
    public static ProcessHandler startProcess(
        @NotNull GeneralCommandLine commandLine,
        @NotNull Project project,
        @Nullable TargetEnvironmentConfiguration config,
        boolean processColors,
        boolean uploadExecutable
    ) throws com.intellij.execution.ExecutionException {
        return TargetUtil.startProcess(commandLine, project, config, processColors, uploadExecutable);
    }
}
