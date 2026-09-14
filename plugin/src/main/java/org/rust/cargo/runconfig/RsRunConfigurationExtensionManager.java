/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.cargo.runconfig;

import consulo.annotation.component.ComponentScope;
import consulo.annotation.component.ServiceAPI;
import consulo.annotation.component.ServiceImpl;
import consulo.execution.configuration.CommandLineState;
import consulo.execution.configuration.RunConfigurationExtensionsManager;
import consulo.execution.runner.ExecutionEnvironment;
import consulo.process.ProcessHandler;
import consulo.process.cmd.GeneralCommandLine;
import jakarta.annotation.Nonnull;
import org.rust.cargo.runconfig.command.CargoCommandConfiguration;

@ServiceAPI(ComponentScope.APPLICATION)
@ServiceImpl
public final class RsRunConfigurationExtensionManager
    extends RunConfigurationExtensionsManager<CargoCommandConfiguration, CargoCommandConfigurationExtension> {

    public RsRunConfigurationExtensionManager() {
        super(CargoCommandConfigurationExtension.class);
    }

    public void attachExtensionsToProcess(
        @Nonnull CargoCommandConfiguration configuration,
        @Nonnull ProcessHandler handler,
        @Nonnull ExecutionEnvironment environment,
        @Nonnull ConfigurationExtensionContext context
    ) {
        for (CargoCommandConfigurationExtension ext : getEnabledExtensions(configuration, environment.getRunnerSettings())) {
            ext.attachToProcess(configuration, handler, environment, context);

        }
    }

    public void patchCommandLine(
        @Nonnull CargoCommandConfiguration configuration,
        @Nonnull ExecutionEnvironment environment,
        @Nonnull GeneralCommandLine cmdLine,
        @Nonnull ConfigurationExtensionContext context
    ) {
        for (CargoCommandConfigurationExtension ext : getEnabledExtensions(configuration, environment.getRunnerSettings())) {
            ext.patchCommandLine(configuration, environment, cmdLine, context);

        }
    }

    public void patchCommandLineState(
        @Nonnull CargoCommandConfiguration configuration,
        @Nonnull ExecutionEnvironment environment,
        @Nonnull CommandLineState state,
        @Nonnull ConfigurationExtensionContext context
    ) {
        for (CargoCommandConfigurationExtension ext : getEnabledExtensions(configuration, environment.getRunnerSettings())) {
            ext.patchCommandLineState(configuration, environment, state, context);

        }
    }

    @Nonnull
    public static RsRunConfigurationExtensionManager getInstance() {
        return consulo.application.ApplicationManager.getApplication()
            .getService(RsRunConfigurationExtensionManager.class);
    }
}
