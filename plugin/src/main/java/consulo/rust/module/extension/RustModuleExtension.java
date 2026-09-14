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
import consulo.annotation.access.RequiredReadAction;
import consulo.rust.bundle.RustBundleType;
import consulo.util.lang.StringUtil;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import org.jdom.Element;
import org.rust.cargo.toolchain.RsToolchainBase;

/**
 * Marks a module as a Rust module and binds it to a Rust toolchain bundle.
 */
public class RustModuleExtension extends ModuleExtensionWithSdkBase<RustModuleExtension> {

    private static final String BUILD_TARGET_ATTRIBUTE = "build-target";

    /** @see #getBuildTarget() */
    protected String myBuildTarget;

    public RustModuleExtension(@Nonnull String id, @Nonnull ModuleRootLayer moduleRootLayer) {
        super(id, moduleRootLayer);
    }

    /**
     * The target triple this module is viewed as, e.g. {@code x86_64-pc-windows-msvc}, or
     * {@code null} to follow {@code .cargo/config.toml} and otherwise the toolchain host.
     * <p>
     * It lives with the module rather than in workspace settings so that it survives a project
     * reopen and travels with the module layer, like the toolchain bundle beside it.
     */
    @Nullable
    public String getBuildTarget() {
        return StringUtil.nullize(myBuildTarget, true);
    }

    @Override
    @RequiredReadAction
    public void commit(RustModuleExtension mutableModuleExtension) {
        super.commit(mutableModuleExtension);
        myBuildTarget = mutableModuleExtension.myBuildTarget;
    }

    @Override
    protected void getStateImpl(@Nonnull Element element) {
        super.getStateImpl(element);
        String buildTarget = getBuildTarget();
        if (buildTarget != null) {
            element.setAttribute(BUILD_TARGET_ATTRIBUTE, buildTarget);
        }
    }

    @Override
    @RequiredReadAction
    protected void loadStateImpl(@Nonnull Element element) {
        super.loadStateImpl(element);
        myBuildTarget = element.getAttributeValue(BUILD_TARGET_ATTRIBUTE);
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
     * The Rust extension of the module owning {@code file}, or {@code null} when there is none.
     */
    @Nullable
    @RequiredReadAction
    public static RustModuleExtension findExtension(@Nonnull consulo.project.Project project,
                                                    @Nullable consulo.virtualFileSystem.VirtualFile file) {
        return file == null ? null : findExtension(ModuleUtilCore.findModuleForFile(file, project));
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
