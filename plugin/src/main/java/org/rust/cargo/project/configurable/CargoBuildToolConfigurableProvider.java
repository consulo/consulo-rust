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

public class CargoBuildToolConfigurableProvider extends ConfigurableProvider {

    private final Project project;

    public CargoBuildToolConfigurableProvider(@Nonnull Project project) {
        this.project = project;
    }

    public boolean canCreateConfigurable() {
        // Only offer the page when a Cargo settings page can actually be built for this project.
        return CargoConfigurable.buildToolsConfigurableExists(project);
    }

    @Nullable
    @Override
    public Configurable createConfigurable() {
        return new CargoConfigurable(project, false);
    }
}
