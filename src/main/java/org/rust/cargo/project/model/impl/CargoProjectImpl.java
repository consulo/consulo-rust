/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.cargo.project.model.impl;

import org.rust.stdext.Lazy;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.UserDataHolderBase;
import com.intellij.openapi.vfs.LocalFileSystem;
import com.intellij.openapi.vfs.VirtualFile;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.TestOnly;
import org.rust.cargo.project.model.CargoProject;
import org.rust.cargo.project.model.RustcInfo;
import org.rust.cargo.project.settings.RsProjectSettingsServiceUtil;
import org.rust.cargo.project.workspace.CargoWorkspace;
import org.rust.cargo.project.workspace.PackageOrigin;
import org.rust.cargo.project.workspace.StandardLibrary;
import org.rust.cargo.toolchain.RsToolchainBase;
import org.rust.lang.core.macros.proc.ProcMacroServerPool;
import org.rust.cargo.util.AutoInjectedCrates;
import org.rust.cargo.runconfig.command.CargoCommandConfiguration;
import org.rust.openapiext.OpenApiUtil;
import org.rust.openapiext.TaskResult;

import java.nio.file.Path;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicReference;

public class CargoProjectImpl extends UserDataHolderBase implements CargoProject {

    @NotNull
    private final Path manifest;

    @NotNull
    private final CargoProjectsServiceImpl projectService;

    @NotNull
    private final UserDisabledFeatures userDisabledFeatures;

    @Nullable
    private final CargoWorkspace rawWorkspace;

    @Nullable
    private final StandardLibrary stdlib;

    @Nullable
    private final RustcInfo rustcInfo;

    @NotNull
    private final UpdateStatus workspaceStatus;

    @NotNull
    private final UpdateStatus stdlibStatus;

    @NotNull
    private final UpdateStatus rustcInfoStatus;

    @Nullable
    private final Path procMacroExpanderPath;

    // Lazy fields
    private volatile CargoWorkspace workspace;
    private volatile boolean workspaceInitialized;

    private volatile String presentableName;
    private volatile boolean presentableNameInitialized;

    private final AtomicReference<VirtualFile> rootDirCache = new AtomicReference<>();

    public CargoProjectImpl(
        @NotNull Path manifest,
        @NotNull CargoProjectsServiceImpl projectService,
        @NotNull UserDisabledFeatures userDisabledFeatures,
        @Nullable CargoWorkspace rawWorkspace,
        @Nullable StandardLibrary stdlib,
        @Nullable RustcInfo rustcInfo,
        @NotNull UpdateStatus workspaceStatus,
        @NotNull UpdateStatus stdlibStatus,
        @NotNull UpdateStatus rustcInfoStatus
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
            RsToolchainBase toolchain = RsProjectSettingsServiceUtil.getToolchain(getProject());
            this.procMacroExpanderPath = toolchain != null
                ? ProcMacroServerPool.findExpanderExecutablePath(toolchain, rustcInfo.getSysroot())
                : null;
        } else {
            this.procMacroExpanderPath = null;
        }
    }

    public CargoProjectImpl(@NotNull Path manifest, @NotNull CargoProjectsServiceImpl projectService,
                            @NotNull UserDisabledFeatures userDisabledFeatures) {
        this(manifest, projectService, userDisabledFeatures, null, null, null,
            UpdateStatus.NeedsUpdate.INSTANCE, UpdateStatus.NeedsUpdate.INSTANCE, UpdateStatus.NeedsUpdate.INSTANCE);
    }

    public CargoProjectImpl(@NotNull Path manifest, @NotNull CargoProjectsServiceImpl projectService) {
        this(manifest, projectService, UserDisabledFeatures.EMPTY);
    }

    @NotNull
    @Override
    public Project getProject() {
        return projectService.getProject();
    }

    @NotNull
    @Override
    public Path getManifest() {
        return manifest;
    }

    @Nullable
    public CargoWorkspace getRawWorkspace() {
        return rawWorkspace;
    }

    @Nullable
    @Override
    public CargoWorkspace getWorkspace() {
        if (!workspaceInitialized) {
            synchronized (this) {
                if (!workspaceInitialized) {
                    workspace = computeWorkspace();
                    workspaceInitialized = true;
                }
            }
        }
        return workspace;
    }

    private CargoWorkspace computeWorkspace() {
        if (rawWorkspace == null) return null;
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

    @NotNull
    @Override
    public String getPresentableName() {
        if (!presentableNameInitialized) {
            synchronized (this) {
                if (!presentableNameInitialized) {
                    presentableName = computePresentableName();
                    presentableNameInitialized = true;
                }
            }
        }
        return presentableName;
    }

    private String computePresentableName() {
        CargoWorkspace ws = getWorkspace();
        if (ws != null) {
            Path workingDir = CargoCommandConfiguration.getWorkingDirectory(this);
            for (CargoWorkspace.Package pkg : ws.getPackages()) {
                if (pkg.getOrigin() == PackageOrigin.WORKSPACE && pkg.getRootDirectory().equals(workingDir)) {
                    return pkg.getName();
                }
            }
        }
        Path workingDir = CargoCommandConfiguration.getWorkingDirectory(this);
        return workingDir.getFileName().toString();
    }

    @Nullable
    @Override
    public VirtualFile getRootDir() {
        VirtualFile cached = rootDirCache.get();
        if (cached != null && cached.isValid()) return cached;
        Path workingDir = CargoCommandConfiguration.getWorkingDirectory(this);
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

    @NotNull
    @Override
    public UpdateStatus getWorkspaceStatus() {
        return workspaceStatus;
    }

    @NotNull
    @Override
    public UpdateStatus getStdlibStatus() {
        return stdlibStatus;
    }

    @NotNull
    @Override
    public UpdateStatus getRustcInfoStatus() {
        return rustcInfoStatus;
    }

    @NotNull
    @Override
    public UserDisabledFeatures getUserDisabledFeatures() {
        return userDisabledFeatures;
    }

    @TestOnly
    public void setRootDir(@NotNull VirtualFile dir) {
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

    public CargoProjectImpl withStdlib(@NotNull TaskResult<StandardLibrary> result) {
        if (result instanceof TaskResult.Ok<StandardLibrary> ok) {
            return copy(null, ok.getValue(), null, null, UpdateStatus.UpToDate.INSTANCE, null);
        } else if (result instanceof TaskResult.Err err) {
            return copy(null, null, null, null, new UpdateStatus.UpdateFailed(err.getReason()), null);
        }
        return this;
    }

    public CargoProjectImpl withWorkspace(@NotNull TaskResult<CargoWorkspace> result) {
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

    public CargoProjectImpl withRustcInfo(@NotNull TaskResult<RustcInfo> result) {
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
        @NotNull UserDisabledFeatures userDisabledFeatures
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
