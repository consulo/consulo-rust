/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.cargo.runconfig.wasmpack;

import com.intellij.execution.configurations.ConfigurationFactory;
import com.intellij.execution.configurations.RunConfiguration;
import com.intellij.openapi.project.Project;
import org.jetbrains.annotations.NotNull;

public class WasmPackConfigurationFactory extends ConfigurationFactory {

    public static final String ID = "wasm-pack";

    public WasmPackConfigurationFactory(@NotNull WasmPackCommandConfigurationType type) {
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
        return new WasmPackCommandConfiguration(project, "wasm-pack", this);
    }
}
