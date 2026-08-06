/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.cargo.runconfig;

import consulo.execution.configuration.RunConfigurationExtensionBase;
import consulo.execution.configuration.CommandLineState;
import consulo.process.cmd.GeneralCommandLine;
import consulo.execution.configuration.RunnerSettings;
import consulo.process.ProcessHandler;
import consulo.execution.runner.ExecutionEnvironment;
import consulo.logging.Logger;
import consulo.component.extension.ExtensionPointName;
import jakarta.annotation.Nonnull;
import org.rust.cargo.runconfig.command.CargoCommandConfiguration;

public abstract class CargoCommandConfigurationExtension extends RunConfigurationExtensionBase<CargoCommandConfiguration> {

    public static final ExtensionPointName<CargoCommandConfigurationExtension> EP_NAME =
        ExtensionPointName.create(CargoCommandConfigurationExtension.class);

    private static final Logger LOG = Logger.getInstance(CargoCommandConfigurationExtension.class);

    public abstract void attachToProcess(
        @Nonnull CargoCommandConfiguration configuration,
        @Nonnull ProcessHandler handler,
        @Nonnull ExecutionEnvironment environment,
        @Nonnull ConfigurationExtensionContext context
    );

    public abstract void patchCommandLine(
        @Nonnull CargoCommandConfiguration configuration,
        @Nonnull ExecutionEnvironment environment,
        @Nonnull GeneralCommandLine cmdLine,
        @Nonnull ConfigurationExtensionContext context
    );

    public void patchCommandLineState(
        @Nonnull CargoCommandConfiguration configuration,
        @Nonnull ExecutionEnvironment environment,
        @Nonnull CommandLineState state,
        @Nonnull ConfigurationExtensionContext context
    ) {
    }

    @Override
    protected void patchCommandLine(
        @Nonnull CargoCommandConfiguration configuration,
        RunnerSettings runnerSettings,
        @Nonnull GeneralCommandLine cmdLine,
        @Nonnull String runnerId
    ) {
        LOG.error("use the other overload of 'patchCommandLine' method");
    }
}
