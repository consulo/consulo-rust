/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.ide.module;

import com.intellij.ide.util.projectWizard.ModuleBuilder.ModuleConfigurationUpdater;
import com.intellij.ide.util.projectWizard.ModuleWizardStep;
import com.intellij.ide.util.projectWizard.WizardContext;
import consulo.module.Module;
import consulo.configurable.ConfigurationException;
import consulo.module.content.layer.ModifiableRootModel;
import consulo.disposer.Disposer;
import consulo.virtualFileSystem.VirtualFile;
import consulo.ui.ex.awt.JBUI;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import org.rust.cargo.CargoConstants;
import org.rust.cargo.project.model.CargoProjectServiceUtil;
import org.rust.cargo.project.settings.RsProjectSettingsServiceUtil;
import org.rust.cargo.project.settings.ui.RustProjectSettingsPanel;
import org.rust.ide.newProject.ConfigurationData;
import org.rust.ide.newProject.ui.RsNewProjectPanel;
import org.rust.openapiext.OpenApiUtil;

import javax.swing.*;
import java.util.function.Consumer;
import org.rust.openapiext.UiUtil;
import org.rust.stdext.BuilderUtil;

public class CargoConfigurationWizardStep extends ModuleWizardStep {

    private final WizardContext context;
    @Nullable
    private final Consumer<ModuleConfigurationUpdater> configurationUpdaterConsumer;
    private final RsNewProjectPanel newProjectPanel;

    public CargoConfigurationWizardStep(@Nonnull WizardContext context) {
        this(context, null);
    }

    public CargoConfigurationWizardStep(
        @Nonnull WizardContext context,
        @Nullable Consumer<ModuleConfigurationUpdater> configurationUpdaterConsumer
    ) {
        this.context = context;
        this.configurationUpdaterConsumer = configurationUpdaterConsumer;
        this.newProjectPanel = new RsNewProjectPanel(configurationUpdaterConsumer == null);
    }

    @Nonnull
    @Override
    public JComponent getComponent() {
        JPanel panel = BuilderUtil.panel(builder -> {
            newProjectPanel.attachTo(builder);
            return null;
        });
        return withBorderIfNeeded(panel);
    }

    @Override
    public void disposeUIResources() {
        Disposer.dispose(newProjectPanel);
    }

    @Override
    public void updateDataModel() {
        ConfigurationData data = newProjectPanel.getData();
        ConfigurationUpdater.data = data.getSettings();

        Object projectBuilder = context.getProjectBuilder();
        if (projectBuilder instanceof RsModuleBuilder rsModuleBuilder) {
            rsModuleBuilder.setConfigurationData(data);
            rsModuleBuilder.addModuleConfigurationUpdater(ConfigurationUpdater.INSTANCE);
        } else if (configurationUpdaterConsumer != null) {
            configurationUpdaterConsumer.accept(ConfigurationUpdater.INSTANCE);
        }
    }

    @Override
    public boolean validate() throws ConfigurationException {
        newProjectPanel.validateSettings();
        return true;
    }

    // It's simple hack to imitate new UI style if new project wizard is enabled
    // TODO: drop it and support new project wizard properly
    //  see https://github.com/intellij-rust/intellij-rust/issues/8585
    @Nonnull
    private <T extends JComponent> T withBorderIfNeeded(@Nonnull T component) {
        if (isNewWizard()) {
            // border size is taken from `com.intellij.ide.wizard.NewProjectWizardStepPanel`
            component.setBorder(JBUI.Borders.empty(14, 20));
        }
        return component;
    }

    private boolean isNewWizard() {
        return OpenApiUtil.isFeatureEnabled("new.project.wizard");
    }

    private static final class ConfigurationUpdater extends ModuleConfigurationUpdater {
        static final ConfigurationUpdater INSTANCE = new ConfigurationUpdater();
        @Nullable
        static RustProjectSettingsPanel.Data data;

        @Override
        public void update(@Nonnull Object moduleObj, @Nonnull ModifiableRootModel rootModel) {
            Module module = (Module) moduleObj;
            RustProjectSettingsPanel.Data currentData = data;
            if (currentData != null) {
                RsProjectSettingsServiceUtil.getRustSettings(module.getProject()).modify(state -> {
                    state.setToolchain(currentData.getToolchain());
                    state.explicitPathToStdlib = currentData.getExplicitPathToStdlib();
                });
            }
            // We don't use SDK, but let's inherit one to reduce the amount of
            // "SDK not configured" errors
            // https://github.com/intellij-rust/intellij-rust/issues/1062
            // rootModel.inheritSdk() — not available in Consulo ModifiableRootModel

            var contentEntries = rootModel.getContentEntries();
            if (contentEntries.length == 1) {
                var contentEntry = contentEntries[0];
                VirtualFile file = contentEntry.getFile();
                if (file != null) {
                    VirtualFile manifest = file.findChild(CargoConstants.MANIFEST_FILE);
                    if (manifest != null) {
                        CargoProjectServiceUtil.getCargoProjects(module.getProject())
                            .attachCargoProject(OpenApiUtil.getPathAsPath(manifest));
                    }
                }

                VirtualFile projectRoot = contentEntry.getFile();
                if (projectRoot != null) {
                    CargoProjectServiceUtil.setup(contentEntry, projectRoot);
                }
            }
        }
    }
}
