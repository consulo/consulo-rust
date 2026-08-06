/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.cargo.runconfig.command;

import consulo.execution.configuration.ConfigurationFactory;
import consulo.execution.configuration.RunConfiguration;
import consulo.project.Project;
import jakarta.annotation.Nonnull;

public class CargoConfigurationFactory extends ConfigurationFactory {
    public static final String ID = "Cargo Command";

    public CargoConfigurationFactory(CargoCommandConfigurationType type) {
        super(type);
    }

    @Nonnull
    @Override
    public String getId() {
        return ID;
    }

    @Nonnull
    @Override
    public RunConfiguration createTemplateConfiguration(@Nonnull Project project) {
        return new CargoCommandConfiguration(project, "Cargo", this);
    }
}
