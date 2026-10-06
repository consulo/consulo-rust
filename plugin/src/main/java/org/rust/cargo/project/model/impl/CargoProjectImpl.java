/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.cargo.project.model.impl;

import org.rust.cargo.api.model.UserDisabledFeatures;

import org.rust.cargo.toolchain.RsToolchainLocator;
import org.rust.stdext.Lazy;
import consulo.module.Module;
import consulo.module.ModuleManager;
import consulo.project.Project;
import consulo.rust.module.extension.RustModuleExtension;
import consulo.util.dataholder.UserDataHolderBase;
import consulo.virtualFileSystem.LocalFileSystem;
import consulo.virtualFileSystem.VirtualFile;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

import org.rust.cargo.api.model.CargoProject;
import org.rust.cargo.api.model.RustcInfo;
import org.rust.cargo.api.settings.RsProjectSettingsServiceUtil;
import org.rust.cargo.api.CargoConfig;
import org.rust.cargo.api.CfgOptions;
import org.rust.cargo.api.workspace.CargoWorkspace;
import org.rust.cargo.api.workspace.PackageOrigin;
import org.rust.cargo.api.workspace.StandardLibrary;
import org.rust.cargo.toolchain.RsToolchainBase;
import org.rust.cargo.macros.ProcMacroServerPool;
import org.rust.cargo.api.util.AutoInjectedCrates;
import org.rust.cargo.runconfig.command.CargoCommandConfiguration;
import org.rust.cargo.project.workspace.CargoWorkspaceFactory;
import org.rust.cargo.project.workspace.state.CargoWorkspaceState;
import org.rust.cargo.project.workspace.state.CargoWorkspaceStates;
import org.rust.openapiext.OpenApiUtil;
import org.rust.openapiext.TaskResult;

import java.nio.file.Path;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicReference;

public class CargoProjectImpl extends UserDataHolderBase implements CargoProject {

    @Nonnull
    private final Path manifest;

    @Nonnull
    private final CargoProjectsServiceImpl projectService;

    @Nonnull
    private final UserDisabledFeatures userDisabledFeatures;

    @Nullable
    private final CargoWorkspace rawWorkspace;

    @Nullable
    private final StandardLibrary stdlib;

    @Nullable
    private final RustcInfo rustcInfo;

    @Nonnull
    private final UpdateStatus workspaceStatus;

    @Nonnull
    private final UpdateStatus stdlibStatus;

    @Nonnull
    private final UpdateStatus rustcInfoStatus;

    @Nullable
    private final Path procMacroExpanderPath;

    // Lazy fields. Both are computed from the project model, which is not loaded yet when the first
    // caller arrives, so both hold a value only once there is a real one to hold - see getWorkspace().
    private volatile CargoWorkspace workspace;
    private volatile String presentableName;

    private final AtomicReference<VirtualFile> rootDirCache = new AtomicReference<>();

    public CargoProjectImpl(
        @Nonnull Path manifest,
        @Nonnull CargoProjectsServiceImpl projectService,
        @Nonnull UserDisabledFeatures userDisabledFeatures,
        @Nullable CargoWorkspace rawWorkspace,
        @Nullable StandardLibrary stdlib,
        @Nullable RustcInfo rustcInfo,
        @Nonnull UpdateStatus workspaceStatus,
        @Nonnull UpdateStatus stdlibStatus,
        @Nonnull UpdateStatus rustcInfoStatus
    ) {
        this.manifest = manifest;
        this.projectService = projectService;
        this.userDisabledFeatures = userDisabledFeatures;
        this.rawWorkspace = rawWorkspace;
        this.stdlib = stdlib;
        this.rustcInfo = rustcInfo;
        this.workspaceStatus = workspaceStatus;
        this.stdlibStatus = stdlibStatus;
        this.rustcInfoStatus = rustcInfoStatus;

        if (rustcInfo != null) {
            RsToolchainBase toolchain = RsToolchainLocator.getToolchain(getProject());
            this.procMacroExpanderPath = toolchain != null
                ? ProcMacroServerPool.findExpanderExecutablePath(toolchain, rustcInfo.getSysroot())
                : null;
        } else {
            this.procMacroExpanderPath = null;
        }
    }

    public CargoProjectImpl(@Nonnull Path manifest, @Nonnull CargoProjectsServiceImpl projectService,
                            @Nonnull UserDisabledFeatures userDisabledFeatures) {
        this(manifest, projectService, userDisabledFeatures, null, null, null,
            UpdateStatus.NeedsUpdate.INSTANCE, UpdateStatus.NeedsUpdate.INSTANCE, UpdateStatus.NeedsUpdate.INSTANCE);
    }

    public CargoProjectImpl(@Nonnull Path manifest, @Nonnull CargoProjectsServiceImpl projectService) {
        this(manifest, projectService, UserDisabledFeatures.EMPTY);
    }

    @Nonnull
    @Override
    public Project getProject() {
        return projectService.getProject();
    }

    @Nonnull
    @Override
    public Path getManifest() {
        return manifest;
    }

    @Nullable
    public CargoWorkspace getRawWorkspace() {
        return rawWorkspace;
    }

    /**
     * Absence is deliberately not cached, so there is no "already computed" flag: the field being null
     * <em>is</em> the not-computed state.
     * <p>
     * A restored workspace is read out of the module extensions, and the first caller routinely arrives
     * before that model is loaded - the publish {@code loadState} schedules alone wakes the tool window,
     * the status bar and the editor notifications, and each of them asks. Remembering the null they see
     * would be permanent, because opening a project no longer runs Cargo: the project would spend the
     * whole session with no crate graph, no run line marker on {@code fn main} and no run configuration.
     */
    @Nullable
    @Override
    public CargoWorkspace getWorkspace() {
        CargoWorkspace computed = workspace;
        if (computed != null) return computed;

        synchronized (this) {
            computed = workspace;
            if (computed == null) {
                computed = computeWorkspace();
                workspace = computed;
            }
        }
        return computed;
    }

    private CargoWorkspace computeWorkspace() {
        if (rawWorkspace == null) {
            // nothing resolved in this session yet: rebuild what the last sync wrote into the project
            // model, so that opening a project does not have to run Cargo
            return restoredWorkspace();
        }
        if (stdlib == null) {
            if (!userDisabledFeatures.isEmpty() && OpenApiUtil.isUnitTestMode()) {
                return rawWorkspace.withDisabledFeatures(userDisabledFeatures);
            }
            return rawWorkspace;
        }
        return rawWorkspace
            .withStdlib(stdlib, rawWorkspace.getCfgOptions(), rustcInfo)
            .withDisabledFeatures(userDisabledFeatures);
    }

    /**
     * The workspace rebuilt from the project model, or {@code null} when nothing was persisted for this
     * Cargo project - a project that has never been synced, which is the one case that still has to run
     * Cargo.
     * <p>
     * Read lazily rather than in {@code loadState}, because the module model is not loaded yet at that
     * point.
     */
    @Nullable
    private CargoWorkspace restoredWorkspace() {
        Project project = projectService.getProject();
        if (project.isDisposed()) return null;

        String manifestPath = manifest.toString();
        for (Module module : ModuleManager.getInstance(project).getModules()) {
            if (module.isDisposed()) continue;
            RustModuleExtension extension = RustModuleExtension.findExtension(module);
            if (extension == null) continue;
            if (!manifestPath.equals(extension.getCargoManifestPath())) continue;

            CargoWorkspaceState state = extension.getCargoWorkspaceState();
            if (state == null) continue;

            CfgOptions cfgOptions = CargoWorkspaceStates.cfgOptionsOf(state);
            return CargoWorkspaceFactory.deserialize(
                manifest,
                CargoWorkspaceStates.fromState(state),
                cfgOptions == null ? CfgOptions.DEFAULT : cfgOptions,
                CargoConfig.DEFAULT
            );
        }
        return null;
    }

    /** Cached only once the workspace has named the package; the directory fallback stays retryable. */
    @Nonnull
    @Override
    public String getPresentableName() {
        String cached = presentableName;
        if (cached != null) return cached;

        String packageName = packageNameFromWorkspace();
        if (packageName == null) {
            return workingDirectoryName();
        }
        presentableName = packageName;
        return packageName;
    }

    @Nullable
    private String packageNameFromWorkspace() {
        CargoWorkspace ws = getWorkspace();
        if (ws == null) return null;

        Path workingDir = org.rust.cargo.project.model.CargoProjectLocator.getWorkingDirectory(this);
        for (CargoWorkspace.Package pkg : ws.getPackages()) {
            if (pkg.getOrigin() == PackageOrigin.WORKSPACE && pkg.getRootDirectory().equals(workingDir)) {
                return pkg.getName();
            }
        }
        return null;
    }

    private String workingDirectoryName() {
        return org.rust.cargo.project.model.CargoProjectLocator.getWorkingDirectory(this).getFileName().toString();
    }

    @Nullable
    @Override
    public VirtualFile getRootDir() {
        VirtualFile cached = rootDirCache.get();
        if (cached != null && cached.isValid()) return cached;
        Path workingDir = org.rust.cargo.project.model.CargoProjectLocator.getWorkingDirectory(this);
        VirtualFile file = LocalFileSystem.getInstance().findFileByIoFile(workingDir.toFile());
        rootDirCache.set(file);
        return file;
    }

    @Nullable
    @Override
    public VirtualFile getWorkspaceRootDir() {
        return rawWorkspace != null ? rawWorkspace.getWorkspaceRoot() : null;
    }

    @Nullable
    @Override
    public RustcInfo getRustcInfo() {
        return rustcInfo;
    }

    @Nullable
    @Override
    public Path getProcMacroExpanderPath() {
        return procMacroExpanderPath;
    }

    @Nonnull
    @Override
    public UpdateStatus getWorkspaceStatus() {
        return workspaceStatus;
    }

    @Nonnull
    @Override
    public UpdateStatus getStdlibStatus() {
        return stdlibStatus;
    }

    @Nonnull
    @Override
    public UpdateStatus getRustcInfoStatus() {
        return rustcInfoStatus;
    }

    @Nonnull
    @Override
    public UserDisabledFeatures getUserDisabledFeatures() {
        return userDisabledFeatures;
    }

    
    public void setRootDir(@Nonnull VirtualFile dir) {
        rootDirCache.set(dir);
    }

    /**
     * Checks that the project is https://github.com/rust-lang/rust
     */
    public boolean doesProjectLooksLikeRustc() {
        CargoWorkspace ws = rawWorkspace;
        if (ws == null) return false;
        List<String> possiblePackages = List.of("rustc", "rustc_middle", "rustc_typeck");
        return ws.findPackageByName(AutoInjectedCrates.STD, null) != null &&
            ws.findPackageByName(AutoInjectedCrates.CORE, null) != null &&
            possiblePackages.stream().anyMatch(name -> ws.findPackageByName(name, null) != null);
    }

    public CargoProjectImpl withStdlib(@Nonnull TaskResult<StandardLibrary> result) {
        if (result instanceof TaskResult.Ok<StandardLibrary> ok) {
            return copy(null, ok.getValue(), null, null, UpdateStatus.UpToDate.INSTANCE, null);
        } else if (result instanceof TaskResult.Err err) {
            return copy(null, null, null, null, new UpdateStatus.UpdateFailed(err.getReason()), null);
        }
        return this;
    }

    public CargoProjectImpl withWorkspace(@Nonnull TaskResult<CargoWorkspace> result) {
        if (result instanceof TaskResult.Ok<CargoWorkspace> ok) {
            return new CargoProjectImpl(manifest, projectService,
                userDisabledFeatures.retain(ok.getValue().getPackages()),
                ok.getValue(), stdlib, rustcInfo,
                UpdateStatus.UpToDate.INSTANCE, stdlibStatus, rustcInfoStatus);
        } else if (result instanceof TaskResult.Err err) {
            return new CargoProjectImpl(manifest, projectService, userDisabledFeatures,
                rawWorkspace, stdlib, rustcInfo,
                new UpdateStatus.UpdateFailed(err.getReason()), stdlibStatus, rustcInfoStatus);
        }
        return this;
    }

    public CargoProjectImpl withRustcInfo(@Nonnull TaskResult<RustcInfo> result) {
        if (result instanceof TaskResult.Ok<RustcInfo> ok) {
            return new CargoProjectImpl(manifest, projectService, userDisabledFeatures,
                rawWorkspace, stdlib, ok.getValue(),
                workspaceStatus, stdlibStatus, UpdateStatus.UpToDate.INSTANCE);
        } else if (result instanceof TaskResult.Err err) {
            return new CargoProjectImpl(manifest, projectService, userDisabledFeatures,
                rawWorkspace, stdlib, rustcInfo,
                workspaceStatus, stdlibStatus, new UpdateStatus.UpdateFailed(err.getReason()));
        }
        return this;
    }

    /**
     * Copy with selective overrides (null means keep existing).
     */
    private CargoProjectImpl copy(
        @Nullable CargoWorkspace newRawWorkspace,
        @Nullable StandardLibrary newStdlib,
        @Nullable RustcInfo newRustcInfo,
        @Nullable UpdateStatus newWorkspaceStatus,
        @Nullable UpdateStatus newStdlibStatus,
        @Nullable UpdateStatus newRustcInfoStatus
    ) {
        return new CargoProjectImpl(manifest, projectService, userDisabledFeatures,
            newRawWorkspace != null ? newRawWorkspace : rawWorkspace,
            newStdlib != null ? newStdlib : stdlib,
            newRustcInfo != null ? newRustcInfo : rustcInfo,
            newWorkspaceStatus != null ? newWorkspaceStatus : workspaceStatus,
            newStdlibStatus != null ? newStdlibStatus : stdlibStatus,
            newRustcInfoStatus != null ? newRustcInfoStatus : rustcInfoStatus
        );
    }

    public CargoProjectImpl copy(
        @Nonnull UserDisabledFeatures userDisabledFeatures
    ) {
        return new CargoProjectImpl(manifest, projectService, userDisabledFeatures,
            rawWorkspace, stdlib, rustcInfo, workspaceStatus, stdlibStatus, rustcInfoStatus);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof CargoProjectImpl that)) return false;
        return Objects.equals(manifest, that.manifest) &&
            Objects.equals(userDisabledFeatures, that.userDisabledFeatures) &&
            Objects.equals(rawWorkspace, that.rawWorkspace) &&
            Objects.equals(stdlib, that.stdlib) &&
            Objects.equals(rustcInfo, that.rustcInfo) &&
            Objects.equals(workspaceStatus, that.workspaceStatus) &&
            Objects.equals(stdlibStatus, that.stdlibStatus) &&
            Objects.equals(rustcInfoStatus, that.rustcInfoStatus);
    }

    @Override
    public int hashCode() {
        return Objects.hash(manifest, userDisabledFeatures, rawWorkspace, stdlib, rustcInfo,
            workspaceStatus, stdlibStatus, rustcInfoStatus);
    }

    @Override
    public String toString() {
        return "CargoProject(manifest = " + manifest + ")";
    }
}
