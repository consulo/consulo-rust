/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.cargo.project.model.impl;

import com.intellij.openapi.components.Service;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.rust.cargo.CargoConstants;
import org.rust.cargo.project.model.CargoProject;
import org.rust.cargo.project.model.CargoProjectServiceUtil;
import org.rust.cargo.project.workspace.CargoWorkspace;
import org.rust.cargo.project.workspace.PackageOrigin;
import org.rust.ide.experiments.RsExperiments;
import org.rust.lang.RsFileType;
import org.rust.lang.core.macros.proc.ProcMacroApplicationService;
import org.rust.openapiext.OpenApiUtil;

import java.util.*;

@Service
public final class CargoSettingsFilesService {

    private final Project project;
    private volatile Map<String, SettingFileType> settingsFilesCache;

    public CargoSettingsFilesService(@NotNull Project project) {
        this.project = project;
    }

    @NotNull
    public Map<String, SettingFileType> collectSettingsFiles(boolean useCache) {
        if (useCache) {
            Map<String, SettingFileType> cached = settingsFilesCache;
            return cached != null ? cached : collectSettingsFilesInternal();
        }
        return collectSettingsFilesInternal();
    }

    private Map<String, SettingFileType> collectSettingsFilesInternal() {
        Map<String, SettingFileType> result = new HashMap<>();
        for (CargoProject cargoProject : CargoProjectServiceUtil.getCargoProjects(project).getAllProjects()) {
            collectCargoProjectSettingsFiles(cargoProject, result);
        }
        settingsFilesCache = result;
        return result;
    }

    private void collectCargoProjectSettingsFiles(@NotNull CargoProject cargoProject, @NotNull Map<String, SettingFileType> out) {
        VirtualFile rootDir = cargoProject.getRootDir();
        if (rootDir != null) {
            String rootPath = rootDir.getPath();
            out.put(rootPath + "/" + CargoConstants.MANIFEST_FILE, SettingFileType.CONFIG);
            out.put(rootPath + "/" + CargoConstants.LOCK_FILE, SettingFileType.CONFIG);
            out.put(rootPath + "/" + CargoConstants.TOOLCHAIN_FILE, SettingFileType.CONFIG);
            out.put(rootPath + "/" + CargoConstants.TOOLCHAIN_TOML_FILE, SettingFileType.CONFIG);
            out.put(rootPath + "/.cargo/" + CargoConstants.CONFIG_FILE, SettingFileType.CONFIG);
            out.put(rootPath + "/.cargo/" + CargoConstants.CONFIG_TOML_FILE, SettingFileType.CONFIG);
        }

        CargoWorkspace workspace = cargoProject.getWorkspace();
        if (workspace != null) {
            for (CargoWorkspace.Package pkg : workspace.getPackages()) {
                if (pkg.getOrigin() == PackageOrigin.WORKSPACE) {
                    collectPackageSettingsFiles(pkg, out);
                }
            }
        }
    }

    private void collectPackageSettingsFiles(@NotNull CargoWorkspace.Package pkg, @NotNull Map<String, SettingFileType> out) {
        VirtualFile root = pkg.getContentRoot();
        if (root == null) return;
        out.put(root.getPath() + "/" + CargoConstants.MANIFEST_FILE, SettingFileType.CONFIG);

        Set<VirtualFile> implicitTargets = collectImplicitTargets(pkg);
        for (VirtualFile target : implicitTargets) {
            out.put(target.getPath(), SettingFileType.IMPLICIT_TARGET);
        }

        if (OpenApiUtil.isFeatureEnabled(RsExperiments.EVALUATE_BUILD_SCRIPTS)) {
            VirtualFile buildScriptFile = findBuildScriptFile(pkg);
            if (buildScriptFile != null) {
                out.put(buildScriptFile.getPath(), SettingFileType.CONFIG);
            }
        }

        if (ProcMacroApplicationService.isAnyEnabled()) {
            for (CargoWorkspace.Target target : pkg.getTargets()) {
                if (target.getKind().isProcMacro()) {
                    VirtualFile crateRoot = target.getCrateRoot();
                    if (crateRoot != null) {
                        out.put(crateRoot.getPath(), SettingFileType.CONFIG);
                    }
                    break;
                }
            }
        }
    }

    @NotNull
    public static CargoSettingsFilesService getInstance(@NotNull Project project) {
        return project.getService(CargoSettingsFilesService.class);
    }

    private static final List<String> IMPLICIT_TARGET_FILES = List.of(
        "src/main.rs", "src/lib.rs"
    );

    private static final List<String> IMPLICIT_TARGET_DIRS = List.of(
        "src/bin", "examples", "tests", "benches"
    );

    @NotNull
    public static Set<VirtualFile> collectImplicitTargets(@NotNull CargoWorkspace.Package pkg) {
        VirtualFile root = pkg.getContentRoot();
        if (root == null) return Collections.emptySet();
        Set<VirtualFile> out = new HashSet<>();

        for (String targetFileName : IMPLICIT_TARGET_FILES) {
            VirtualFile file = root.findFileByRelativePath(targetFileName);
            if (file != null) out.add(file);
        }

        VirtualFile buildScriptFile = findBuildScriptFile(pkg);
        if (buildScriptFile != null) {
            out.add(buildScriptFile);
        }

        for (String targetDirName : IMPLICIT_TARGET_DIRS) {
            VirtualFile dir = root.findFileByRelativePath(targetDirName);
            if (dir == null) continue;
            for (VirtualFile file : dir.getChildren()) {
                if (!file.isDirectory() && file.getFileType() == RsFileType.INSTANCE) {
                    out.add(file);
                }
            }
        }

        return out;
    }

    @Nullable
    private static VirtualFile findBuildScriptFile(@NotNull CargoWorkspace.Package pkg) {
        for (CargoWorkspace.Target target : pkg.getTargets()) {
            if (target.getKind().isCustomBuild()) {
                return target.getCrateRoot();
            }
        }
        VirtualFile contentRoot = pkg.getContentRoot();
        return contentRoot != null ? contentRoot.findFileByRelativePath(CargoConstants.BUILD_FILE) : null;
    }

    public enum SettingFileType {
        CONFIG,
        IMPLICIT_TARGET
    }
}
