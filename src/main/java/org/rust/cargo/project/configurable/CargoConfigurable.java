/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.cargo.project.configurable;

import com.intellij.ide.DataManager;
import com.intellij.openapi.externalSystem.service.settings.ExternalSystemGroupConfigurable;
import com.intellij.openapi.options.Configurable;
import com.intellij.openapi.options.ex.Settings;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.DialogPanel;
import com.intellij.ui.dsl.builder.BottomGap;
import org.jetbrains.annotations.NotNull;
import org.rust.RsBundle;
import org.rust.cargo.project.model.CargoProjectServiceUtil;
import org.rust.cargo.project.settings.RsProjectSettingsServiceUtil;

import java.awt.*;

public class CargoConfigurable extends RsConfigurableBase {

    private final boolean isPlaceholder;

    public CargoConfigurable(@NotNull Project project, boolean isPlaceholder) {
        super(project, RsBundle.message("settings.rust.cargo.name"));
        this.isPlaceholder = isPlaceholder;
    }

    @NotNull
    @Override
    public DialogPanel createPanel() {
        return isPlaceholder ? createPlaceholderPanel() : createSettingsPanel();
    }

    private DialogPanel createSettingsPanel() {
        return new DialogPanel();
    }

    private DialogPanel createPlaceholderPanel() {
        return new DialogPanel();
    }

    private void openCargoSettings(Component component) {
        com.intellij.openapi.actionSystem.DataContext dataContext = DataManager.getInstance().getDataContext(component);
        Settings settings = Settings.KEY.getData(dataContext);
        if (settings != null) {
            Configurable configurable = settings.find("language.rust.build.tool.cargo");
            settings.select(configurable);
        }
    }

    public static boolean buildToolsConfigurableExists(@NotNull Project project) {
        com.intellij.openapi.options.ConfigurableEP<Configurable> buildToolsConfigurable =
            Configurable.PROJECT_CONFIGURABLE.findFirstSafe(project, it -> "build.tools".equals(it.id));
        return buildToolsConfigurable != null;
    }
}
