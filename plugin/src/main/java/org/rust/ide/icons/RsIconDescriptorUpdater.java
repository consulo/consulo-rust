/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.ide.icons;

import consulo.annotation.component.ExtensionImpl;
import consulo.language.icon.IconDescriptor;
import consulo.language.icon.IconDescriptorUpdater;
import consulo.language.psi.PsiElement;
import consulo.platform.base.icon.PlatformIconGroup;
import consulo.ui.image.ImageEffects;
import jakarta.annotation.Nonnull;
import org.rust.lang.core.psi.RsConstant;
import org.rust.lang.core.psi.RsEnumItem;
import org.rust.lang.core.psi.RsEnumVariant;
import org.rust.lang.core.psi.ext.RsFieldDecl;
import org.rust.lang.core.psi.RsFunction;
import org.rust.lang.core.psi.RsImplItem;
import org.rust.lang.core.psi.RsMacro;
import org.rust.lang.core.psi.RsModDeclItem;
import org.rust.lang.core.psi.RsModItem;
import org.rust.lang.core.psi.RsPatBinding;
import org.rust.lang.core.psi.RsStructItem;
import org.rust.lang.core.psi.RsTraitAlias;
import org.rust.lang.core.psi.RsTraitItem;
import org.rust.lang.core.psi.RsTypeAlias;
import consulo.rust.icon.RustIconGroup;
import consulo.ui.image.Image;
import consulo.language.psi.PsiFile;
import jakarta.annotation.Nullable;
import org.rust.lang.RsConstants;
import org.rust.lang.core.psi.impl.RsFile;
import org.rust.cargo.CargoConstants;
import org.rust.cargo.api.workspace.CargoWorkspace;

/**
 * Supplies the icon for every Rust PSI element, and for the Cargo manifest and lock files.
 */
@ExtensionImpl
public class RsIconDescriptorUpdater implements IconDescriptorUpdater {
    public static final Image RUST_MAIN = ImageEffects.layered(RustIconGroup.rustfile(), PlatformIconGroup.nodesRunnablemark());
    
    @Override
    public void updateIcon(@Nonnull IconDescriptor iconDescriptor, @Nonnull PsiElement element, int flags) {
        if (element instanceof RsFunction) {
            iconDescriptor.setMainIcon(RustIconGroup.nodesFunction());
        } else if (element instanceof RsStructItem) {
            iconDescriptor.setMainIcon(RustIconGroup.nodesStruct());
        } else if (element instanceof RsTraitItem) {
            iconDescriptor.setMainIcon(RustIconGroup.nodesTrait());
        } else if (element instanceof RsTraitAlias) {
            iconDescriptor.setMainIcon(RustIconGroup.nodesTypealias());
        } else if (element instanceof RsEnumItem) {
            iconDescriptor.setMainIcon(RustIconGroup.nodesEnum());
        } else if (element instanceof RsEnumVariant) {
            iconDescriptor.setMainIcon(RustIconGroup.nodesEnumvariant());
        } else if (element instanceof RsImplItem) {
            iconDescriptor.setMainIcon(RustIconGroup.nodesImpl());
        } else if (element instanceof RsModItem || element instanceof RsModDeclItem) {
            iconDescriptor.setMainIcon(RustIconGroup.nodesModule());
        } else if (element instanceof RsConstant) {
            iconDescriptor.setMainIcon(RustIconGroup.nodesConstant());
        } else if (element instanceof RsTypeAlias) {
            iconDescriptor.setMainIcon(RustIconGroup.nodesTypealias());
        } else if (element instanceof RsMacro) {
            iconDescriptor.setMainIcon(RustIconGroup.nodesMacro());
        } else if (element instanceof RsFieldDecl) {
            iconDescriptor.setMainIcon(RustIconGroup.nodesField());
        } else if (element instanceof RsPatBinding) {
            iconDescriptor.setMainIcon(RustIconGroup.nodesField());
        } else if (element instanceof RsFile) {
            Image fileIcon = fileIcon((RsFile) element);
            if (fileIcon != null) {
                iconDescriptor.setMainIcon(fileIcon);
            }
        } else if (element instanceof PsiFile) {
            Image manifestIcon = manifestIcon(((PsiFile) element).getName());
            if (manifestIcon != null) {
                iconDescriptor.setMainIcon(manifestIcon);
            }
        }
    }

    @Nullable
    private static Image fileIcon(@Nonnull RsFile file) {
        String name = file.getName();
        if (RsConstants.MOD_RS_FILE.equals(name)) {
            // i think we not need it
        }
        if ((RsConstants.MAIN_RS_FILE.equals(name) || RsConstants.LIB_RS_FILE.equals(name)) && file.isCrateRoot()) {
            return RUST_MAIN;
        }
        if (file.isCrateRoot() && file.getCrate().getKind() == CargoWorkspace.TargetKind.CustomBuild.INSTANCE) {
            return RustIconGroup.rustbuild();
        }
        return RustIconGroup.rustfile();
    }

    @Nullable
    private static Image manifestIcon(@Nonnull String fileName) {
        if (CargoConstants.MANIFEST_FILE.equals(fileName) || CargoConstants.XARGO_MANIFEST_FILE.equals(fileName)) {
            return RustIconGroup.cargo();
        }
        if (CargoConstants.LOCK_FILE.equals(fileName)) {
            return RustIconGroup.cargolock();
        }
        return null;
    }
}
