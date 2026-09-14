/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package consulo.rust.module.extension;

import consulo.content.bundle.Sdk;
import consulo.disposer.Disposable;
import consulo.module.content.layer.ModuleRootLayer;
import consulo.module.extension.MutableModuleExtensionWithSdk;
import consulo.module.extension.MutableModuleInheritableNamedPointer;
import consulo.module.ui.extension.ModuleExtensionBundleBoxBuilder;
import consulo.ui.Component;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.layout.VerticalLayout;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

import java.util.Objects;

/**
 * Editable counterpart of {@link RustModuleExtension}, shown in the module settings dialog.
 */
public class RustMutableModuleExtension extends RustModuleExtension implements MutableModuleExtensionWithSdk<RustModuleExtension> {

    public RustMutableModuleExtension(@Nonnull String id, @Nonnull ModuleRootLayer moduleRootLayer) {
        super(id, moduleRootLayer);
    }

    @Nonnull
    @Override
    public MutableModuleInheritableNamedPointer<Sdk> getInheritableSdk() {
        return (MutableModuleInheritableNamedPointer<Sdk>) super.getInheritableSdk();
    }

    @RequiredUIAccess
    @Nullable
    @Override
    public Component createConfigurationComponent(@Nonnull Disposable disposable, @Nonnull Runnable runnable) {
        VerticalLayout layout = VerticalLayout.create();
        layout.add(ModuleExtensionBundleBoxBuilder.createAndDefine(this, disposable, runnable).build());
        return layout;
    }

    @Override
    public void setEnabled(boolean enabled) {
        myIsEnabled = enabled;
    }

    /** @see RustModuleExtension#getBuildTarget() */
    public void setBuildTarget(@Nullable String buildTarget) {
        myBuildTarget = buildTarget;
    }

    @Override
    public boolean isModified(@Nonnull RustModuleExtension extension) {
        return isModifiedImpl(extension)
            || !Objects.equals(getBuildTarget(), extension.getBuildTarget());
    }
}
