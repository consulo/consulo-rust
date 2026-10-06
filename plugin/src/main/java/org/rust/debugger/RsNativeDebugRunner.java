/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.debugger;

import consulo.annotation.component.ExtensionImpl;
import consulo.execution.configuration.RunProfile;
import consulo.execution.debug.DefaultDebugExecutor;
import consulo.execution.runner.ExecutionEnvironment;
import consulo.execution.ui.RunContentDescriptor;
import consulo.nativeDev.debugger.NativeDebugSessionStarter;
import consulo.nativeDev.debugger.driver.NativeDebugTarget;
import consulo.process.ExecutionException;
import consulo.process.cmd.GeneralCommandLine;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import org.rust.cargo.api.model.CargoProject;
import org.rust.cargo.runconfig.CargoRunStateBase;
import org.rust.cargo.runconfig.RsExecutableRunner;
import org.rust.debugger.runconfig.RsDebugProcessConfigurationHelper;

import java.io.File;
import java.nio.file.Path;

@ExtensionImpl
public class RsNativeDebugRunner extends RsExecutableRunner {
    public static final String RUNNER_ID = "RsNativeDebugRunner";

    @Nonnull
    @Override
    public String getRunnerId() {
        return RUNNER_ID;
    }

    @Override
    public boolean canRun(@Nonnull String executorId, @Nonnull RunProfile profile) {
        return DefaultDebugExecutor.EXECUTOR_ID.equals(executorId) && super.canRun(executorId, profile);
    }

    @Nullable
    @Override
    protected RunContentDescriptor showRunContent(@Nonnull CargoRunStateBase state,
                                                  @Nonnull ExecutionEnvironment environment,
                                                  @Nonnull GeneralCommandLine runExecutable) throws ExecutionException {
        CargoProject cargoProject = state.getCargoProject();
        File executableWorkDirectory = runExecutable.getWorkDirectory();
        NativeDebugTarget target = NativeDebugTarget.launch(
            Path.of(runExecutable.getExePath()),
            runExecutable.getParametersList().getList(),
            executableWorkDirectory == null ? null : executableWorkDirectory.toPath(),
            runExecutable.getEnvironment(),
            null
        ).withSetup(new RsDebugProcessConfigurationHelper(state.getToolchain(), cargoProject));
        return NativeDebugSessionStarter.getInstance().startSession(environment, target, null);
    }
}
