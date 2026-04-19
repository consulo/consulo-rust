/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.cargo.project.configurable;

import com.intellij.openapi.options.Configurable;
import com.intellij.openapi.options.ConfigurableProvider;
import com.intellij.openapi.project.Project;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class CargoProjectConfigurableProvider extends ConfigurableProvider {

    private final Project project;

    public CargoProjectConfigurableProvider(@NotNull Project project) {
        this.project = project;
    }

    @Nullable
    @Override
    public Configurable createConfigurable() {
        boolean isPlaceholder = CargoConfigurable.buildToolsConfigurableExists(project);
        return new CargoConfigurable(project, isPlaceholder);
    }
}
