/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.cargo.runconfig.target;

import consulo.process.cmd.GeneralCommandLine;
import consulo.process.ProcessHandler;
import consulo.project.Project;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import org.rust.cargo.runconfig.command.CargoCommandConfiguration;

import java.util.List;
import consulo.process.ExecutionException;

/**
 * Bridge class delegating to {@link TargetUtil}; target environment configurations are untyped ({@code Object}).
 */
public final class UtilsUtil {

    private UtilsUtil() {
    }

    @Nullable
    public static Object getTargetEnvironment(@Nonnull CargoCommandConfiguration config) {
        return TargetUtil.getTargetEnvironment(config);
    }

    @Nonnull
    public static List<String> getLocalBuildArgsForRemoteRun(@Nonnull CargoCommandConfiguration config) {
        return TargetUtil.getLocalBuildArgsForRemoteRun(config);
    }

    @Nullable
    public static Object getLanguageRuntime(@Nonnull Object config) {
        return TargetUtil.getLanguageRuntime(config);
    }

    @Nonnull
    public static ProcessHandler startProcess(
        @Nonnull GeneralCommandLine commandLine,
        @Nonnull Project project,
        @Nullable Object config,
        boolean processColors,
        boolean uploadExecutable
    ) throws ExecutionException {
        return TargetUtil.startProcess(commandLine, project, config, processColors, uploadExecutable);
    }
}
