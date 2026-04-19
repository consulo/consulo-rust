/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.cargo.runconfig;

import com.intellij.execution.configuration.RunConfigurationExtensionBase;
import com.intellij.execution.configurations.CommandLineState;
import com.intellij.execution.configurations.GeneralCommandLine;
import com.intellij.execution.configurations.RunnerSettings;
import com.intellij.execution.process.ProcessHandler;
import com.intellij.execution.runners.ExecutionEnvironment;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.extensions.ExtensionPointName;
import org.jetbrains.annotations.NotNull;
import org.rust.cargo.runconfig.command.CargoCommandConfiguration;

public abstract class CargoCommandConfigurationExtension extends RunConfigurationExtensionBase<CargoCommandConfiguration> {

    public static final ExtensionPointName<CargoCommandConfigurationExtension> EP_NAME =
        ExtensionPointName.create("org.rust.runConfigurationExtension");

    private static final Logger LOG = Logger.getInstance(CargoCommandConfigurationExtension.class);

    public abstract void attachToProcess(
        @NotNull CargoCommandConfiguration configuration,
        @NotNull ProcessHandler handler,
        @NotNull ExecutionEnvironment environment,
        @NotNull ConfigurationExtensionContext context
    );

    public abstract void patchCommandLine(
        @NotNull CargoCommandConfiguration configuration,
        @NotNull ExecutionEnvironment environment,
        @NotNull GeneralCommandLine cmdLine,
        @NotNull ConfigurationExtensionContext context
    );

    public void patchCommandLineState(
        @NotNull CargoCommandConfiguration configuration,
        @NotNull ExecutionEnvironment environment,
        @NotNull CommandLineState state,
        @NotNull ConfigurationExtensionContext context
    ) {
    }

    @Override
    protected void patchCommandLine(
        @NotNull CargoCommandConfiguration configuration,
        RunnerSettings runnerSettings,
        @NotNull GeneralCommandLine cmdLine,
        @NotNull String runnerId
    ) {
        LOG.error("use the other overload of 'patchCommandLine' method");
    }
}
