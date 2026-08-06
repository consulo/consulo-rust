/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.cargo.runconfig;

import consulo.execution.configuration.RunConfigurationExtensionsManager;
import consulo.execution.configuration.CommandLineState;
import consulo.process.cmd.GeneralCommandLine;
import consulo.process.ProcessHandler;
import consulo.execution.runner.ExecutionEnvironment;
import com.intellij.openapi.components.Service;
import jakarta.annotation.Nonnull;
import org.rust.cargo.runconfig.command.CargoCommandConfiguration;

import static com.intellij.openapi.components.Service.Level;

@Service(Level.APP)
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
