/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.cargo.project.configurable;

import consulo.annotation.component.ExtensionImpl;
import consulo.configurable.Configurable;
import consulo.configurable.ConfigurationException;
import consulo.configurable.ProjectConfigurable;
import consulo.configurable.StandardConfigurableIds;
import consulo.project.Project;
import com.intellij.openapi.ui.DialogPanel;
import consulo.disposer.Disposer;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import jakarta.inject.Inject;
import org.rust.RsBundle;
import org.rust.cargo.project.model.CargoProject;
import org.rust.cargo.project.model.CargoProjectServiceUtil;
import org.rust.cargo.project.settings.RsProjectSettingsServiceUtil;
import org.rust.cargo.project.settings.ui.RustProjectSettingsPanel;
import org.rust.cargo.project.settings.RustProjectSettingsService;
import org.rust.cargo.toolchain.RsToolchainBase;
import org.rust.openapiext.OpenApiUtil;

import java.awt.BorderLayout;
import java.nio.file.Path;
import java.nio.file.Paths;

@ExtensionImpl
public class RsProjectConfigurable extends RsConfigurableBase implements ProjectConfigurable, Configurable.NoScroll {

    /** Settings-tree id other Rust pages hang off as children. */
    public static final String ID = "language.rust";

    private final Path projectDir;
    private volatile RustProjectSettingsPanel rustProjectSettings;

    @Inject
    public RsProjectConfigurable(@Nonnull Project project) {
        super(project, RsBundle.message("settings.rust.toolchain.name"));
        CargoProject firstProject = CargoProjectServiceUtil.getCargoProjects(project).getAllProjects()
            .stream().findFirst().orElse(null);
        this.projectDir = firstProject != null && firstProject.getRootDir() != null
            ? OpenApiUtil.getPathAsPath(firstProject.getRootDir())
            : Paths.get(".");
    }

    @Nonnull
    @Override
    public String getId() {
        return ID;
    }

    @Nullable
    @Override
    public String getParentId() {
        return StandardConfigurableIds.EXECUTION_GROUP;
    }

    private RustProjectSettingsPanel getRustProjectSettings() {
        if (rustProjectSettings == null) {
            rustProjectSettings = new RustProjectSettingsPanel(projectDir, null);
        }
        return rustProjectSettings;
    }

    @Nonnull
    @Override
    public DialogPanel createPanel() {
        RustProjectSettingsPanel settingsPanel = getRustProjectSettings();
        RustProjectSettingsService settings = RsProjectSettingsServiceUtil.getRustSettings(project);

        DialogPanel panel = new DialogPanel(new BorderLayout());
        panel.add(settingsPanel.getComponent(), BorderLayout.NORTH);

        panel.bind(
            () -> settingsPanel.setData(currentData(settings)),
            () -> {
                RustProjectSettingsPanel.Data data = settingsPanel.getData();
                settings.modify(state -> {
                    RsToolchainBase toolchain = data.getToolchain();
                    state.toolchainHomeDirectory = toolchain != null ? toolchain.getLocation().toString() : null;
                    state.explicitPathToStdlib = data.getExplicitPathToStdlib();
                });
            },
            () -> !settingsPanel.getData().equals(currentData(settings)));

        settingsPanel.loadToolchains();
        return panel;
    }

    @Nonnull
    private static RustProjectSettingsPanel.Data currentData(@Nonnull RustProjectSettingsService settings) {
        return new RustProjectSettingsPanel.Data(settings.getToolchain(), settings.getExplicitPathToStdlib());
    }

    @Override
    public void disposeUIResources() {
        super.disposeUIResources();
        RustProjectSettingsPanel panel = rustProjectSettings;
        if (panel != null) {
            rustProjectSettings = null;
            Disposer.dispose(panel);
        }
    }

    @Override
    public void apply() throws ConfigurationException {
        RustProjectSettingsPanel panel = rustProjectSettings;
        if (panel != null) {
            panel.validateSettings();
        }
        super.apply();
    }
}
