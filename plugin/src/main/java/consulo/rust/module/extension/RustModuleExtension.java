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
import consulo.util.jdom.JDOMUtil;
import consulo.util.xml.serializer.XmlSerializer;
import org.jdom.Element;
import org.rust.cargo.project.workspace.state.CargoWorkspaceState;
import org.rust.cargo.toolchain.RsToolchainBase;

/**
 * Marks a module as a Rust module and binds it to a Rust toolchain bundle.
 */
public class RustModuleExtension extends ModuleExtensionWithSdkBase<RustModuleExtension> {

    private static final String BUILD_TARGET_ATTRIBUTE = "build-target";
    private static final String PACKAGE_ID_ATTRIBUTE = "cargo-package-id";
    private static final String MANIFEST_ATTRIBUTE = "cargo-manifest";
    private static final String WORKSPACE_ELEMENT = "cargo-workspace";

    /** @see #getBuildTarget() */
    protected String myBuildTarget;

    /** @see #getCargoPackageId() */
    protected String myCargoPackageId;

    /** @see #getCargoManifestPath() */
    protected String myCargoManifestPath;

    /** @see #getCargoWorkspaceState() */
    protected CargoWorkspaceState myCargoWorkspaceState;

    /**
     * Serialized form of {@link #myCargoWorkspaceState}, kept so that "has the workspace changed?" is a
     * string comparison. The state is a plain bean with no equality of its own, and comparing by
     * identity would report a change on every sync and reindex the project each time.
     */
    protected String myCargoWorkspaceFingerprint;

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

    /**
     * Id of the workspace member package this module stands for, or {@code null} when the module was
     * not created from a Cargo workspace member.
     * <p>
     * It is also the mark of a module this plugin owns: only a module carrying one is ever disposed
     * when its member leaves the workspace.
     */
    @Nullable
    public String getCargoPackageId() {
        return StringUtil.nullize(myCargoPackageId, true);
    }

    /**
     * Manifest of the Cargo project this module belongs to, which is what identifies that project
     * across refreshes.
     */
    @Nullable
    public String getCargoManifestPath() {
        return StringUtil.nullize(myCargoManifestPath, true);
    }

    /**
     * The resolved Cargo workspace of the project this module owns, as last written by a sync, or
     * {@code null} for a module that owns no Cargo project.
     * <p>
     * This is what lets a project open without running Cargo: the workspace is rebuilt from here rather
     * than from {@code cargo metadata}.
     */
    @Nullable
    public CargoWorkspaceState getCargoWorkspaceState() {
        return myCargoWorkspaceState;
    }

    /** @see #myCargoWorkspaceFingerprint */
    @Nullable
    public String getCargoWorkspaceFingerprint() {
        return myCargoWorkspaceFingerprint;
    }

    /** The one place a workspace fingerprint is computed, so both writing and reading agree on it. */
    @Nullable
    protected static String fingerprintOf(@Nullable CargoWorkspaceState state) {
        if (state == null) return null;
        Element element = XmlSerializer.serialize(state);
        return element == null ? null : JDOMUtil.writeElement(element);
    }

    @Override
    @RequiredReadAction
    public void commit(RustModuleExtension mutableModuleExtension) {
        super.commit(mutableModuleExtension);
        myBuildTarget = mutableModuleExtension.myBuildTarget;
        myCargoPackageId = mutableModuleExtension.myCargoPackageId;
        myCargoManifestPath = mutableModuleExtension.myCargoManifestPath;
        myCargoWorkspaceState = mutableModuleExtension.myCargoWorkspaceState;
        myCargoWorkspaceFingerprint = mutableModuleExtension.myCargoWorkspaceFingerprint;
    }

    @Override
    protected void getStateImpl(@Nonnull Element element) {
        super.getStateImpl(element);
        String buildTarget = getBuildTarget();
        if (buildTarget != null) {
            element.setAttribute(BUILD_TARGET_ATTRIBUTE, buildTarget);
        }
        String packageId = getCargoPackageId();
        if (packageId != null) {
            element.setAttribute(PACKAGE_ID_ATTRIBUTE, packageId);
        }
        String manifestPath = getCargoManifestPath();
        if (manifestPath != null) {
            element.setAttribute(MANIFEST_ATTRIBUTE, manifestPath);
        }
        if (myCargoWorkspaceState != null) {
            Element workspace = XmlSerializer.serialize(myCargoWorkspaceState);
            if (workspace != null) {
                workspace.setName(WORKSPACE_ELEMENT);
                element.addContent(workspace);
            }
        }
    }

    @Override
    @RequiredReadAction
    protected void loadStateImpl(@Nonnull Element element) {
        super.loadStateImpl(element);
        myBuildTarget = element.getAttributeValue(BUILD_TARGET_ATTRIBUTE);
        myCargoPackageId = element.getAttributeValue(PACKAGE_ID_ATTRIBUTE);
        myCargoManifestPath = element.getAttributeValue(MANIFEST_ATTRIBUTE);
        Element workspace = element.getChild(WORKSPACE_ELEMENT);
        myCargoWorkspaceState = workspace == null ? null : XmlSerializer.deserialize(workspace, CargoWorkspaceState.class);
        myCargoWorkspaceFingerprint = fingerprintOf(myCargoWorkspaceState);
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
