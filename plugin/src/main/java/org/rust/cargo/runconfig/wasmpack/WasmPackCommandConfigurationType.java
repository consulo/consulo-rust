/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.cargo.runconfig.wasmpack;

import consulo.annotation.component.ExtensionImpl;
import consulo.execution.configuration.ConfigurationFactory;
import consulo.execution.configuration.ConfigurationTypeBase;
import consulo.execution.configuration.ConfigurationTypeUtil;
import consulo.execution.configuration.RunConfiguration;
import consulo.project.Project;
import jakarta.annotation.Nonnull;
import org.rust.RsBundle;
import org.rust.ide.icons.RsIcons;
import consulo.localize.LocalizeValue;

@ExtensionImpl
public class WasmPackCommandConfigurationType extends ConfigurationTypeBase {

    public WasmPackCommandConfigurationType() {
        super(
            "WasmPackCommandRunConfiguration",
            consulo.localize.LocalizeValue.of(RsBundle.message("wasm.pack")),
            consulo.localize.LocalizeValue.of(RsBundle.message("wasm.pack.command.run.configuration")),
            RsIcons.WASM_PACK
        );
        addFactory(new WasmPackConfigurationFactory(this));
    }

    @Nonnull
    public ConfigurationFactory getFactory() {
        return getConfigurationFactories()[0];
    }

    @Nonnull
    public static WasmPackCommandConfigurationType getInstance() {
        return ConfigurationTypeUtil.findConfigurationType(WasmPackCommandConfigurationType.class);
    }
}
