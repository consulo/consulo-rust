package com.intellij.ide.util.projectWizard;

import com.intellij.openapi.module.ModuleType;
import consulo.configurable.ConfigurationException;
import consulo.content.bundle.SdkTypeId;
import consulo.disposer.Disposable;
import consulo.module.content.layer.ContentEntry;
import consulo.module.content.layer.ModifiableRootModel;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

/** IntelliJ-compat stub. */
public abstract class ModuleBuilder {
    protected String myName;
    protected String myModuleFilePath;

    public abstract static class ModuleConfigurationUpdater {
        public abstract void update(Object module, ModifiableRootModel rootModel);
    }

    public void addModuleConfigurationUpdater(ModuleConfigurationUpdater updater) {}
    protected ContentEntry doAddContentEntry(ModifiableRootModel model) { return null; }
    public boolean inheritSdk() { return true; }
    public String getName() { return myName; }
    public void setName(String name) { this.myName = name; }
    public String getModuleFilePath() { return myModuleFilePath; }
    public void setModuleFilePath(String path) { this.myModuleFilePath = path; }

    @Nonnull
    public ModuleType<?> getModuleType() { return null; }
    public boolean isSuitableSdkType(@Nullable SdkTypeId sdkType) { return true; }
    public boolean isAvailable() { return true; }

    @Nonnull
    public ModuleWizardStep getCustomOptionsStep(@Nonnull WizardContext context, @Nonnull Disposable parentDisposable) {
        throw new UnsupportedOperationException();
    }

    public void setupRootModel(@Nonnull ModifiableRootModel modifiableRootModel) {}

    public boolean validateModuleName(@Nonnull String moduleName) throws ConfigurationException { return true; }
}
