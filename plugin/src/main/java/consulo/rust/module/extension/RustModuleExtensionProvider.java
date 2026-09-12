/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package consulo.rust.module.extension;

import consulo.annotation.component.ExtensionImpl;
import consulo.localize.LocalizeValue;
import consulo.module.content.layer.ModuleExtensionProvider;
import consulo.module.content.layer.ModuleRootLayer;
import consulo.module.extension.ModuleExtension;
import consulo.module.extension.MutableModuleExtension;
import consulo.rust.icon.RustIconGroup;
import consulo.ui.image.Image;
import jakarta.annotation.Nonnull;

/**
 * Registers the Rust module extension so that modules can be marked as Rust modules.
 */
@ExtensionImpl
public class RustModuleExtensionProvider implements ModuleExtensionProvider<RustModuleExtension> {

    public static final String ID = "rust";

    @Nonnull
    @Override
    public String getId() {
        return ID;
    }

    @Nonnull
    @Override
    public LocalizeValue getName() {
        return LocalizeValue.localizeTODO("Rust");
    }

    @Nonnull
    @Override
    public Image getIcon() {
        return RustIconGroup.rust();
    }

    @Nonnull
    @Override
    public ModuleExtension<RustModuleExtension> createImmutableExtension(@Nonnull ModuleRootLayer moduleRootLayer) {
        return new RustModuleExtension(getId(), moduleRootLayer);
    }

    @Nonnull
    @Override
    public MutableModuleExtension<RustModuleExtension> createMutableExtension(@Nonnull ModuleRootLayer moduleRootLayer) {
        return new RustMutableModuleExtension(getId(), moduleRootLayer);
    }
}
