/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.cargo.project.configurable;

import consulo.annotation.component.ExtensionImpl;
import consulo.dataContext.DataManager;
import consulo.configurable.Configurable;
import consulo.configurable.ProjectConfigurable;
import consulo.configurable.Settings;
import consulo.project.Project;
import com.intellij.openapi.ui.DialogPanel;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import jakarta.inject.Inject;
import org.rust.RsBundle;
import org.rust.cargo.project.model.CargoProjectServiceUtil;
import org.rust.cargo.project.settings.RsProjectSettingsServiceUtil;

import consulo.ui.ex.awt.FormBuilder;
import consulo.ui.ex.awt.LinkLabel;
import org.rust.cargo.project.settings.RustProjectSettingsService;

import javax.swing.*;
import java.awt.*;

@ExtensionImpl
public class CargoConfigurable extends RsConfigurableBase implements ProjectConfigurable {

    private final boolean isPlaceholder;

    @Inject
    public CargoConfigurable(@Nonnull Project project) {
        this(project, false);
    }

    /**
     * @param isPlaceholder when true the page only points at the real Cargo settings
     *                      instead of hosting the editable controls itself.
     */
    public CargoConfigurable(@Nonnull Project project, boolean isPlaceholder) {
        super(project, RsBundle.message("settings.rust.cargo.name"));
        this.isPlaceholder = isPlaceholder;
    }

    @Nonnull
    @Override
    public String getId() {
        return "language.rust.cargo";
    }

    @Nullable
    @Override
    public String getParentId() {
        return RsProjectConfigurable.ID;
    }

    @Nonnull
    @Override
    public DialogPanel createPanel() {
        return isPlaceholder ? createPlaceholderPanel() : createSettingsPanel();
    }

    private DialogPanel createSettingsPanel() {
        RustProjectSettingsService settings = RsProjectSettingsServiceUtil.getRustSettings(project);

        JCheckBox showFirstError = new JCheckBox(RsBundle.message("settings.rust.cargo.show.first.error.label"));
        JCheckBox autoUpdate = new JCheckBox(RsBundle.message("settings.rust.cargo.auto.update.project.label"));
        JCheckBox compileAllTargets = new JCheckBox(RsBundle.message("settings.rust.cargo.compile.all.targets.label"));
        JCheckBox offlineMode = new JCheckBox(RsBundle.message("settings.rust.cargo.offline.mode.label"));

        FormBuilder builder = FormBuilder.createFormBuilder()
            .addComponent(showFirstError);
        // Project model updates are driven by the build-tools settings once the new import is on.
        if (!CargoProjectServiceUtil.isNewProjectModelImportEnabled()) {
            builder = builder.addComponent(autoUpdate);
        }
        JPanel form = builder
            .addComponent(compileAllTargets)
            .addTooltip(RsBundle.message("settings.rust.cargo.compile.all.targets.comment"))
            .addComponent(offlineMode)
            .addTooltip(RsBundle.message("settings.rust.cargo.offline.mode.comment"))
            .getPanel();

        DialogPanel panel = new DialogPanel(new BorderLayout());
        panel.add(form, BorderLayout.NORTH);

        panel.bind(
            () -> showFirstError.setSelected(settings.getAutoShowErrorsInEditor().toBoolean()),
            () -> settings.modify(state -> state.autoShowErrorsInEditor = showFirstError.isSelected()),
            () -> showFirstError.isSelected() != settings.getAutoShowErrorsInEditor().toBoolean());
        panel.bind(
            () -> autoUpdate.setSelected(settings.getAutoUpdateEnabled()),
            () -> settings.modify(state -> state.autoUpdateEnabled = autoUpdate.isSelected()),
            () -> autoUpdate.isSelected() != settings.getAutoUpdateEnabled());
        panel.bind(
            () -> compileAllTargets.setSelected(settings.getCompileAllTargets()),
            () -> settings.modify(state -> state.compileAllTargets = compileAllTargets.isSelected()),
            () -> compileAllTargets.isSelected() != settings.getCompileAllTargets());
        panel.bind(
            () -> offlineMode.setSelected(settings.getUseOffline()),
            () -> settings.modify(state -> state.useOffline = offlineMode.isSelected()),
            () -> offlineMode.isSelected() != settings.getUseOffline());

        return panel;
    }

    private DialogPanel createPlaceholderPanel() {
        DialogPanel panel = new DialogPanel(new BorderLayout());
        LinkLabel<?> link = LinkLabel.create(
            RsBundle.message("settings.rust.cargo.moved.label"),
            () -> openCargoSettings(panel));
        panel.add(link, BorderLayout.CENTER);
        return panel;
    }

    private void openCargoSettings(Component component) {
        Settings settings = DataManager.getInstance().getDataContext(component).getData(Settings.KEY);
        if (settings == null) return;
        Configurable configurable = settings.findConfigurableById("language.rust.build.tool.cargo");
        if (configurable != null) {
            settings.select(configurable);
        }
    }

    public static boolean buildToolsConfigurableExists(@Nonnull Project project) {
        // TODO: Consulo doesn't expose ConfigurableEP.PROJECT_CONFIGURABLE; assume cargo configurable exists
        return true;
    }
}
