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
        // Upstream gated this on IntelliJ >= 2022.2, the release that introduced the Build Tools
        // configurable group. Consulo has no such version boundary, so only the capability check
        // remains.
        return CargoConfigurable.buildToolsConfigurableExists(project);
    }

    @Nullable
    @Override
    public Configurable createConfigurable() {
        return new CargoConfigurable(project, false);
    }
}
