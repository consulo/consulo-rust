/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.cargo.project.configurable;

import consulo.dataContext.DataManager;
import com.intellij.openapi.externalSystem.service.settings.ExternalSystemGroupConfigurable;
import consulo.configurable.Configurable;
import consulo.configurable.Settings;
import consulo.project.Project;
import com.intellij.openapi.ui.DialogPanel;
import com.intellij.ui.dsl.builder.BottomGap;
import jakarta.annotation.Nonnull;
import org.rust.RsBundle;
import org.rust.cargo.project.model.CargoProjectServiceUtil;
import org.rust.cargo.project.settings.RsProjectSettingsServiceUtil;

import java.awt.*;

public class CargoConfigurable extends RsConfigurableBase {

    private final boolean isPlaceholder;

    public CargoConfigurable(@Nonnull Project project, boolean isPlaceholder) {
        super(project, RsBundle.message("settings.rust.cargo.name"));
        this.isPlaceholder = isPlaceholder;
    }

    @Nonnull
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
        // TODO: wire Consulo Settings navigation — find/select by id not directly available here
    }

    public static boolean buildToolsConfigurableExists(@Nonnull Project project) {
        // TODO: Consulo doesn't expose ConfigurableEP.PROJECT_CONFIGURABLE; assume cargo configurable exists
        return true;
    }
}
