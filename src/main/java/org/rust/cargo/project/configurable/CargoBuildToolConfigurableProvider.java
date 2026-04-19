/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.cargo.project.configurable;

import com.intellij.openapi.application.ApplicationInfo;
import com.intellij.openapi.options.Configurable;
import com.intellij.openapi.options.ConfigurableProvider;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.BuildNumber;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class CargoBuildToolConfigurableProvider extends ConfigurableProvider {

    private final Project project;

    // BACKCOMPAT: 2022.1
    private static final BuildNumber BUILD_222 = BuildNumber.fromString("222");

    public CargoBuildToolConfigurableProvider(@NotNull Project project) {
        this.project = project;
    }

    @Override
    public boolean canCreateConfigurable() {
        return ApplicationInfo.getInstance().getBuild().compareTo(BUILD_222) >= 0
            && CargoConfigurable.buildToolsConfigurableExists(project);
    }

    @Nullable
    @Override
    public Configurable createConfigurable() {
        return new CargoConfigurable(project, false);
    }
}
