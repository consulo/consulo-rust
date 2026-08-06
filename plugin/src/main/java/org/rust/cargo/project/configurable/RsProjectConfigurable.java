/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.cargo.project.configurable;

import consulo.configurable.Configurable;
import consulo.configurable.ConfigurationException;
import consulo.project.Project;
import com.intellij.openapi.ui.DialogPanel;
import consulo.disposer.Disposer;
import jakarta.annotation.Nonnull;
import org.rust.RsBundle;
import org.rust.cargo.project.model.CargoProject;
import org.rust.cargo.project.model.CargoProjectServiceUtil;
import org.rust.cargo.project.settings.RsProjectSettingsServiceUtil;
import org.rust.cargo.project.settings.ui.RustProjectSettingsPanel;
import org.rust.cargo.toolchain.RsToolchainBase;
import org.rust.openapiext.OpenApiUtil;

import java.nio.file.Path;
import java.nio.file.Paths;

public class RsProjectConfigurable extends RsConfigurableBase implements Configurable.NoScroll {

    private final Path projectDir;
    private volatile RustProjectSettingsPanel rustProjectSettings;

    public RsProjectConfigurable(@Nonnull Project project) {
        super(project, RsBundle.message("settings.rust.toolchain.name"));
        CargoProject firstProject = CargoProjectServiceUtil.getCargoProjects(project).getAllProjects()
            .stream().findFirst().orElse(null);
        this.projectDir = firstProject != null && firstProject.getRootDir() != null
            ? OpenApiUtil.getPathAsPath(firstProject.getRootDir())
            : Paths.get(".");
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
        return new DialogPanel();
    }

    @Override
    public void disposeUIResources() {
        super.disposeUIResources();
        if (rustProjectSettings != null) {
            Disposer.dispose(rustProjectSettings);
        }
    }

    @Override
    public void apply() {
        try {
            getRustProjectSettings().validateSettings();
        } catch (ConfigurationException e) {
            // Configuration validation failed, but parent apply() doesn't throw
        }
        super.apply();
    }
}
