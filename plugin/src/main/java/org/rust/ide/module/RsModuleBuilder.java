/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.ide.module;

import com.intellij.ide.NewProjectWizardLegacy;
import com.intellij.ide.util.projectWizard.ModuleBuilder;
import com.intellij.ide.util.projectWizard.ModuleWizardStep;
import com.intellij.ide.util.projectWizard.WizardContext;
import consulo.disposer.Disposable;
import consulo.logging.Logger;
import com.intellij.openapi.module.ModuleType;
import consulo.configurable.ConfigurationException;
import consulo.content.bundle.SdkTypeId;
import consulo.module.content.layer.ModifiableRootModel;
import consulo.disposer.Disposer;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import org.rust.cargo.CargoConstants;
import org.rust.cargo.toolchain.tools.Cargo;
import org.rust.ide.newProject.ConfigurationData;
import org.rust.ide.newProject.RsProjectTemplate;
import org.rust.ide.newProject.Utils;
import org.rust.stdext.RsResult;

/**
 * Builder which is used when a new project or module is created and not imported from source.
 */
public class RsModuleBuilder extends ModuleBuilder {

    private static final Logger LOG = Logger.getInstance(RsModuleBuilder.class);

    @Nullable
    private ConfigurationData configurationData;

    @Nonnull
    @Override
    public ModuleType<?> getModuleType() {
        return RsModuleType.INSTANCE;
    }

    @Override
    public boolean isSuitableSdkType(@Nullable SdkTypeId sdkType) {
        return true;
    }

    @Override
    public boolean isAvailable() {
        return NewProjectWizardLegacy.isAvailable();
    }

    @Nonnull
    @Override
    public ModuleWizardStep getCustomOptionsStep(@Nonnull WizardContext context, @Nonnull Disposable parentDisposable) {
        CargoConfigurationWizardStep step = new CargoConfigurationWizardStep(context);
        Disposer.register(parentDisposable, step::disposeUIResources);
        return step;
    }

    @Override
    public void setupRootModel(@Nonnull ModifiableRootModel modifiableRootModel) {
        createProject(modifiableRootModel, "git");
    }

    public void createProject(@Nonnull ModifiableRootModel modifiableRootModel, @Nullable String vcs) {
        var contentEntry = doAddContentEntry(modifiableRootModel);
        if (contentEntry == null) return;
        var root = contentEntry.getFile();
        if (root == null) return;
        // modifiableRootModel.inheritSdk() isn't on Consulo's ModifiableRootModel
        var toolchain = configurationData != null ? configurationData.getSettings().getToolchain() : null;
        root.refresh(/* async = */ false, /* recursive = */ true);

        // Just work if user "creates new project" over an existing one.
        if (toolchain != null && root.findChild(CargoConstants.MANIFEST_FILE) == null) {
            // TODO: rewrite this somehow to fix `Synchronous execution on EDT` exception
            // The problem is that `setupRootModel` is called on EDT under write action
            // so `$ cargo init` invocation blocks UI thread

            if (configurationData == null) return;
            RsProjectTemplate template = configurationData.getTemplate();
            Cargo cargo = Cargo.cargo(toolchain);
            var project = modifiableRootModel.getProject();
            String name = project.getName().replace(' ', '_');

            var result = Utils.makeProject(cargo, project, modifiableRootModel.getModule(), root, name, template, vcs);
            Cargo.GeneratedFilesHolder generatedFiles;
            try {
                generatedFiles = result.unwrapOrElse(err -> {
                    LOG.error(err);
                    throw new RuntimeException(err.getMessage());
                });
            } catch (RuntimeException e) {
                throw new RuntimeException(e.getMessage());
            }

            Utils.makeDefaultRunConfiguration(project, template);
            Utils.openFiles(project, generatedFiles);
        }
    }

    @Override
    public boolean validateModuleName(@Nonnull String moduleName) throws ConfigurationException {
        if (configurationData == null) return true;
        String errorMessage = configurationData.getTemplate().validateProjectName(moduleName);
        if (errorMessage != null) {
            throw new ConfigurationException(errorMessage);
        }
        return true;
    }

    @Nullable
    public ConfigurationData getConfigurationData() {
        return configurationData;
    }

    public void setConfigurationData(@Nullable ConfigurationData configurationData) {
        this.configurationData = configurationData;
    }
}
