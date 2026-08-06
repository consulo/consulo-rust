/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.cargo.runconfig.wasmpack;

import consulo.execution.configuration.ConfigurationFactory;
import consulo.execution.configuration.RunConfiguration;
import consulo.project.Project;
import jakarta.annotation.Nonnull;

public class WasmPackConfigurationFactory extends ConfigurationFactory {

    public static final String ID = "wasm-pack";

    public WasmPackConfigurationFactory(@Nonnull WasmPackCommandConfigurationType type) {
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
        return new WasmPackCommandConfiguration(project, "wasm-pack", this);
    }
}
