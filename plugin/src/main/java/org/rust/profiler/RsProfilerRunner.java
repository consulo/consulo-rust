/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.profiler;

import consulo.annotation.component.ExtensionImpl;
import consulo.execution.configuration.RunProfile;
import consulo.execution.runner.ExecutionEnvironment;
import consulo.execution.ui.RunContentDescriptor;
import consulo.nativeDev.profiler.NativeProfilerLauncher;
import consulo.process.ExecutionException;
import consulo.process.cmd.GeneralCommandLine;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import org.rust.cargo.runconfig.CargoRunStateBase;
import org.rust.cargo.runconfig.RsExecutableRunner;

@ExtensionImpl
public class RsProfilerRunner extends RsExecutableRunner {
    public static final String RUNNER_ID = "RsProfilerRunner";

    @Nonnull
    @Override
    public String getRunnerId() {
        return RUNNER_ID;
    }

    @Override
    public boolean canRun(@Nonnull String executorId, @Nonnull RunProfile profile) {
        return super.canRun(executorId, profile) && NativeProfilerLauncher.canRun(executorId, profile);
    }

    @Nullable
    @Override
    protected RunContentDescriptor showRunContent(@Nonnull CargoRunStateBase state,
                                                  @Nonnull ExecutionEnvironment environment,
                                                  @Nonnull GeneralCommandLine runExecutable) throws ExecutionException {
        return NativeProfilerLauncher.execute(environment, runExecutable);
    }
}
