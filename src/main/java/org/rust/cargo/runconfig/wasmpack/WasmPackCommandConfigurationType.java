/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.cargo.runconfig.wasmpack;

import com.intellij.execution.configurations.ConfigurationFactory;
import com.intellij.execution.configurations.ConfigurationTypeBase;
import com.intellij.execution.configurations.ConfigurationTypeUtil;
import com.intellij.execution.configurations.RunConfiguration;
import com.intellij.openapi.project.Project;
import org.jetbrains.annotations.NotNull;
import org.rust.RsBundle;
import org.rust.ide.icons.RsIcons;

public class WasmPackCommandConfigurationType extends ConfigurationTypeBase {

    public WasmPackCommandConfigurationType() {
        super(
            "WasmPackCommandRunConfiguration",
            RsBundle.message("wasm.pack"),
            RsBundle.message("wasm.pack.command.run.configuration"),
            RsIcons.WASM_PACK
        );
        addFactory(new WasmPackConfigurationFactory(this));
    }

    @NotNull
    public ConfigurationFactory getFactory() {
        return getConfigurationFactories()[0];
    }

    @NotNull
    public static WasmPackCommandConfigurationType getInstance() {
        return ConfigurationTypeUtil.findConfigurationType(WasmPackCommandConfigurationType.class);
    }
}
