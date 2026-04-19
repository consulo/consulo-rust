/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.cargo.runconfig;

import com.intellij.execution.configuration.RunConfigurationExtensionsManager;
import com.intellij.execution.configurations.CommandLineState;
import com.intellij.execution.configurations.GeneralCommandLine;
import com.intellij.execution.process.ProcessHandler;
import com.intellij.execution.runners.ExecutionEnvironment;
import com.intellij.openapi.components.Service;
import org.jetbrains.annotations.NotNull;
import org.rust.cargo.runconfig.command.CargoCommandConfiguration;

import static com.intellij.openapi.components.Service.Level;

@Service(Level.APP)
public final class RsRunConfigurationExtensionManager
    extends RunConfigurationExtensionsManager<CargoCommandConfiguration, CargoCommandConfigurationExtension> {

    public RsRunConfigurationExtensionManager() {
        super(CargoCommandConfigurationExtension.EP_NAME);
    }

    public void attachExtensionsToProcess(
        @NotNull CargoCommandConfiguration configuration,
        @NotNull ProcessHandler handler,
        @NotNull ExecutionEnvironment environment,
        @NotNull ConfigurationExtensionContext context
    ) {
        processEnabledExtensions(configuration, environment.getRunnerSettings(), ext -> {
            ext.attachToProcess(configuration, handler, environment, context);
            return kotlin.Unit.INSTANCE;
        });
    }

    public void patchCommandLine(
        @NotNull CargoCommandConfiguration configuration,
        @NotNull ExecutionEnvironment environment,
        @NotNull GeneralCommandLine cmdLine,
        @NotNull ConfigurationExtensionContext context
    ) {
        processEnabledExtensions(configuration, environment.getRunnerSettings(), ext -> {
            ext.patchCommandLine(configuration, environment, cmdLine, context);
            return kotlin.Unit.INSTANCE;
        });
    }

    public void patchCommandLineState(
        @NotNull CargoCommandConfiguration configuration,
        @NotNull ExecutionEnvironment environment,
        @NotNull CommandLineState state,
        @NotNull ConfigurationExtensionContext context
    ) {
        processEnabledExtensions(configuration, environment.getRunnerSettings(), ext -> {
            ext.patchCommandLineState(configuration, environment, state, context);
            return kotlin.Unit.INSTANCE;
        });
    }

    @NotNull
    public static RsRunConfigurationExtensionManager getInstance() {
        return com.intellij.openapi.application.ApplicationManager.getApplication()
            .getService(RsRunConfigurationExtensionManager.class);
    }
}
