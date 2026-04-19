/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.cargo.runconfig.buildtool;

import com.intellij.execution.runners.ExecutionEnvironment;
import com.intellij.openapi.roots.ProjectModelBuildableElement;
import com.intellij.openapi.roots.ProjectModelExternalSource;
import org.jetbrains.annotations.Nullable;
import org.rust.cargo.runconfig.command.CargoCommandConfiguration;

@SuppressWarnings("UnstableApiUsage")
public class CargoBuildConfiguration implements ProjectModelBuildableElement {
    private final CargoCommandConfiguration configuration;
    private final ExecutionEnvironment environment;

    public CargoBuildConfiguration(CargoCommandConfiguration configuration, ExecutionEnvironment environment) {
        if (!CargoBuildManager.INSTANCE.isBuildConfiguration(configuration)) {
            throw new IllegalArgumentException();
        }
        this.configuration = configuration;
        this.environment = environment;
    }

    public CargoCommandConfiguration getConfiguration() {
        return configuration;
    }

    public ExecutionEnvironment getEnvironment() {
        return environment;
    }

    public boolean getEnabled() {
        return CargoBuildManager.INSTANCE.isBuildToolWindowAvailable(configuration);
    }

    @Nullable
    @Override
    public ProjectModelExternalSource getExternalSource() {
        return null;
    }
}
