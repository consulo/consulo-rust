/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.cargo.project.configurable;

import consulo.configurable.Configurable;
import consulo.configurable.ConfigurableProvider;
import consulo.project.Project;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

public class CargoProjectConfigurableProvider extends ConfigurableProvider {

    private final Project project;

    public CargoProjectConfigurableProvider(@Nonnull Project project) {
        this.project = project;
    }

    @Nullable
    @Override
    public Configurable createConfigurable() {
        boolean isPlaceholder = CargoConfigurable.buildToolsConfigurableExists(project);
        return new CargoConfigurable(project, isPlaceholder);
    }
}
