/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.cargo.runconfig.command;

import consulo.annotation.component.ExtensionImpl;
import consulo.execution.configuration.ConfigurationFactory;
import consulo.execution.configuration.ConfigurationTypeBase;
import consulo.execution.configuration.ConfigurationTypeUtil;
import consulo.execution.configuration.RunConfiguration;
import consulo.project.Project;
import com.intellij.util.PlatformUtils;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import org.rust.RsBundle;
import org.rust.ide.icons.RsIcons;
import consulo.localize.LocalizeValue;

@ExtensionImpl
public class CargoCommandConfigurationType extends ConfigurationTypeBase {

    public CargoCommandConfigurationType() {
        super(
            "CargoCommandRunConfiguration",
            consulo.localize.LocalizeValue.of(RsBundle.message("build.event.title.cargo")),
            consulo.localize.LocalizeValue.of(RsBundle.message("cargo.command.run.configuration")),
            RsIcons.RUST
        );
        addFactory(new CargoConfigurationFactory(this));
    }

    public ConfigurationFactory getFactory() {
        return getConfigurationFactories()[0];
    }

    @Nullable
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
