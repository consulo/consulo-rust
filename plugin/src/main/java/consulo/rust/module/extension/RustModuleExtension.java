/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package consulo.rust.module.extension;

import consulo.content.bundle.SdkType;
import consulo.language.psi.PsiElement;
import consulo.language.util.ModuleUtilCore;
import consulo.module.Module;
import consulo.module.content.layer.ModuleRootLayer;
import consulo.module.content.layer.extension.ModuleExtensionWithSdkBase;
import consulo.rust.bundle.RustBundleType;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import org.rust.cargo.toolchain.RsToolchainBase;

/**
 * Marks a module as a Rust module and binds it to a Rust toolchain bundle.
 */
public class RustModuleExtension extends ModuleExtensionWithSdkBase<RustModuleExtension> {

    public RustModuleExtension(@Nonnull String id, @Nonnull ModuleRootLayer moduleRootLayer) {
        super(id, moduleRootLayer);
    }

    @Nonnull
    @Override
    public Class<? extends SdkType> getSdkTypeClass() {
        return RustBundleType.class;
    }

    /**
     * The toolchain this module is bound to, or {@code null} when no bundle is selected or the
     * selected bundle no longer points at a usable toolchain.
     */
    @Nullable
    public RsToolchainBase getToolchain() {
        return RustBundleType.toToolchain(getSdk());
    }

    /**
     * The Rust extension of {@code module}, or {@code null} when the module is not a Rust module.
     */
    @Nullable
    public static RustModuleExtension findExtension(@Nullable Module module) {
        return module == null ? null : ModuleUtilCore.getExtension(module, RustModuleExtension.class);
    }

    /**
     * The Rust extension of the module owning {@code element}, or {@code null} when there is none.
     */
    @Nullable
    public static RustModuleExtension findExtension(@Nullable PsiElement element) {
        return element == null ? null : ModuleUtilCore.getExtension(element, RustModuleExtension.class);
    }

    /**
     * The toolchain bound to {@code module}, or {@code null} when the module is not a Rust module
     * or carries no usable bundle.
     */
    @Nullable
    public static RsToolchainBase findToolchain(@Nullable Module module) {
        RustModuleExtension extension = findExtension(module);
        return extension == null ? null : extension.getToolchain();
    }
}
