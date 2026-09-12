/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.cargo.runconfig.target;

import consulo.process.ExecutionException;
import consulo.process.ProcessHandler;
import consulo.process.cmd.GeneralCommandLine;
import consulo.execution.process.ProcessTerminatedListener;
import consulo.project.Project;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import org.rust.cargo.runconfig.RsProcessHandler;
import org.rust.cargo.runconfig.command.CargoCommandConfiguration;

import java.util.Collections;
import java.util.List;

/** Run-on-target support: no target environment is ever available, so lookups return nothing and processes start locally. */
public final class TargetUtil {

    private TargetUtil() {
    }

    @Nullable
    public static Object getTargetEnvironment(@Nonnull CargoCommandConfiguration config) {
        return null;
    }

    @Nullable
    public static Object getLanguageRuntime(@Nullable Object targetEnvironment) {
        return null;
    }

    @Nonnull
    public static List<String> getLocalBuildArgsForRemoteRun(@Nonnull CargoCommandConfiguration configuration) {
        return Collections.emptyList();
    }

    public static boolean hasRemoteTarget(@Nonnull CargoCommandConfiguration configuration) {
        return false;
    }

    @Nonnull
    public static ProcessHandler startProcess(@Nonnull GeneralCommandLine commandLine,
                                               @Nonnull Project project,
                                               @Nullable Object targetEnvironment,
                                               boolean processColors,
                                               boolean patchPythonIO) throws ExecutionException {
        RsProcessHandler handler = new RsProcessHandler(commandLine, processColors);
        ProcessTerminatedListener.attach(handler);
        return handler;
    }
}
