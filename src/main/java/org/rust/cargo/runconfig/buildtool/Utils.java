/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.cargo.runconfig.buildtool;

import com.intellij.execution.ExecutionListener;
import com.intellij.execution.ExecutionManager;
import com.intellij.execution.process.ProcessHandler;
import com.intellij.execution.runners.ExecutionEnvironment;
import com.intellij.openapi.progress.EmptyProgressIndicator;
import com.intellij.openapi.util.Key;
import org.jetbrains.annotations.Nullable;
import org.rust.cargo.toolchain.CargoCommandLine;

import java.util.Collections;
import java.util.List;
import java.util.function.Function;

/**
 * Utility class for buildtool-related extension functions and properties.
 */
public final class Utils {

    private static final Key<List<Function<CargoCommandLine, CargoCommandLine>>> CARGO_PATCHES = Key.create("CARGO.PATCHES");

    private Utils() {
    }

    public static List<Function<CargoCommandLine, CargoCommandLine>> getCargoPatches(ExecutionEnvironment environment) {
        List<Function<CargoCommandLine, CargoCommandLine>> patches = environment.putUserDataIfAbsent(CARGO_PATCHES, Collections.emptyList());
        return patches;
    }

    public static void setCargoPatches(ExecutionEnvironment environment, List<Function<CargoCommandLine, CargoCommandLine>> value) {
        environment.putUserData(CARGO_PATCHES, value);
    }

    private static ExecutionListener getExecutionListener(ExecutionEnvironment environment) {
        return environment.getProject().getMessageBus().syncPublisher(ExecutionManager.EXECUTION_TOPIC);
    }

    public static void notifyProcessStartScheduled(ExecutionEnvironment environment) {
        getExecutionListener(environment).processStartScheduled(environment.getExecutor().getId(), environment);
    }

    public static void notifyProcessStarting(ExecutionEnvironment environment) {
        getExecutionListener(environment).processStarting(environment.getExecutor().getId(), environment);
    }

    public static void notifyProcessNotStarted(ExecutionEnvironment environment) {
        getExecutionListener(environment).processNotStarted(environment.getExecutor().getId(), environment);
    }

    public static void notifyProcessStarted(ExecutionEnvironment environment, ProcessHandler handler) {
        getExecutionListener(environment).processStarted(environment.getExecutor().getId(), environment, handler);
    }

    public static void notifyProcessTerminating(ExecutionEnvironment environment, ProcessHandler handler) {
        getExecutionListener(environment).processTerminating(environment.getExecutor().getId(), environment, handler);
    }

    public static void notifyProcessTerminated(ExecutionEnvironment environment, ProcessHandler handler, int exitCode) {
        getExecutionListener(environment).processTerminated(environment.getExecutor().getId(), environment, handler, exitCode);
    }

    public static boolean isActivateToolWindowBeforeRun(@Nullable ExecutionEnvironment environment) {
        if (environment == null) return true;
        var settings = environment.getRunnerAndConfigurationSettings();
        return settings == null || settings.isActivateToolWindowBeforeRun();
    }
}
