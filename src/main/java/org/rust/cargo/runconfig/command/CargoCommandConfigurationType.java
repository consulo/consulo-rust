/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.cargo.runconfig.command;

import com.intellij.execution.configurations.ConfigurationFactory;
import com.intellij.execution.configurations.ConfigurationTypeBase;
import com.intellij.execution.configurations.ConfigurationTypeUtil;
import com.intellij.execution.configurations.RunConfiguration;
import com.intellij.openapi.project.Project;
import com.intellij.util.PlatformUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.rust.RsBundle;
import org.rust.ide.icons.RsIcons;

public class CargoCommandConfigurationType extends ConfigurationTypeBase {

    public CargoCommandConfigurationType() {
        super(
            "CargoCommandRunConfiguration",
            RsBundle.message("build.event.title.cargo"),
            RsBundle.message("cargo.command.run.configuration"),
            RsIcons.RUST
        );
        addFactory(new CargoConfigurationFactory(this));
    }

    public ConfigurationFactory getFactory() {
        return getConfigurationFactories()[0];
    }

    @Nullable
    @Override
    public String getHelpTopic() {
        if (PlatformUtils.isIntelliJ() || PlatformUtils.isCLion()) {
            return "rundebugconfigs.cargocommand";
        }
        return null;
    }

    public static CargoCommandConfigurationType getInstance() {
        return ConfigurationTypeUtil.findConfigurationType(CargoCommandConfigurationType.class);
    }
}
