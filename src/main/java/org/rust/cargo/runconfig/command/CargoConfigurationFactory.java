/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.cargo.runconfig.command;

import com.intellij.execution.configurations.ConfigurationFactory;
import com.intellij.execution.configurations.RunConfiguration;
import com.intellij.openapi.project.Project;
import org.jetbrains.annotations.NotNull;

public class CargoConfigurationFactory extends ConfigurationFactory {
    public static final String ID = "Cargo Command";

    public CargoConfigurationFactory(CargoCommandConfigurationType type) {
        super(type);
    }

    @NotNull
    @Override
    public String getId() {
        return ID;
    }

    @NotNull
    @Override
    public RunConfiguration createTemplateConfiguration(@NotNull Project project) {
        return new CargoCommandConfiguration(project, "Cargo", this);
    }
}
