/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.cargo.project.model;

import com.intellij.ide.util.PropertiesComponent;
import com.intellij.notification.NotificationType;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.application.WriteAction;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.project.ProjectUtil;
import com.intellij.openapi.roots.ContentEntry;
import com.intellij.openapi.util.registry.Registry;
import com.intellij.openapi.vfs.VfsUtil;
import com.intellij.openapi.vfs.VirtualFile;
import org.jetbrains.annotations.NotNull;
import org.rust.RsBundle;
import org.rust.cargo.CargoConstants;
import org.rust.cargo.project.settings.RustProjectSettingsService;
import org.rust.cargo.project.workspace.CargoWorkspace;
import org.rust.cargo.toolchain.RsToolchainBase;
import org.rust.cargo.toolchain.tools.Rustup;
import org.rust.ide.notifications.NotificationUtils;

import java.nio.file.Path;
import java.nio.file.Paths;

public final class CargoProjectServiceUtil {

    private CargoProjectServiceUtil() {
    }

    @NotNull
    public static CargoProjectsService getCargoProjects(@NotNull Project project) {
        return project.getService(CargoProjectsService.class);
    }

    public static boolean isGeneratedFile(@NotNull CargoProjectsService service, @NotNull VirtualFile file) {
        CargoWorkspace.Package pkg = service.findPackageForFile(file);
        if (pkg == null) return false;
        VirtualFile outDir = pkg.getOutDir();
        if (outDir == null) return false;
        return VfsUtil.isAncestor(outDir, file, false);
    }

    public static boolean guessAndSetupRustProject(@NotNull Project project, boolean explicitRequest) {
        if (!explicitRequest) {
            String key = "org.rust.cargo.project.model.PROJECT_DISCOVERY";
            PropertiesComponent properties = PropertiesComponent.getInstance(project);
            boolean alreadyTried = properties.getBoolean(key);
            properties.setValue(key, true);
            if (alreadyTried) return false;
        }

        RustProjectSettingsService settingsService = project.getService(RustProjectSettingsService.class);
        RsToolchainBase toolchain = settingsService.getToolchain();
        if (toolchain == null || !toolchain.looksLikeValidToolchain()) {
            discoverToolchain(project);
            return true;
        }
        if (!getCargoProjects(project).getHasAtLeastOneValidProject()) {
            getCargoProjects(project).discoverAndRefresh();
            return true;
        }
        return false;
    }

    public static boolean guessAndSetupRustProject(@NotNull Project project) {
        return guessAndSetupRustProject(project, false);
    }

    private static void discoverToolchain(@NotNull Project project) {
        VirtualFile projectDir = ProjectUtil.guessProjectDir(project);
        Path projectPath = projectDir != null ? Paths.get(projectDir.getPath()) : null;
        RsToolchainBase toolchain = RsToolchainBase.suggest(projectPath);
        if (toolchain == null) return;

        ApplicationManager.getApplication().invokeLater(() -> {
            if (project.isDisposed()) return;

            RustProjectSettingsService settingsService = project.getService(RustProjectSettingsService.class);
            RsToolchainBase oldToolchain = settingsService.getToolchain();
            if (oldToolchain != null && oldToolchain.looksLikeValidToolchain()) {
                return;
            }

            WriteAction.run(() ->
                settingsService.modify(state -> {
                    state.setToolchain(toolchain);
                })
            );

            String tool = Rustup.isRustupAvailable(toolchain)
                ? RsBundle.message("notification.content.rustup")
                : RsBundle.message("notification.content.cargo.at", toolchain.getPresentableLocation());
            NotificationUtils.showBalloon(project, RsBundle.message("notification.content.using", tool), NotificationType.INFORMATION);
            getCargoProjects(project).discoverAndRefresh();
        });
    }

    public static void setup(@NotNull ContentEntry contentEntry, @NotNull VirtualFile contentRoot) {
        setup(new ContentEntryWrapper(contentEntry), contentRoot);
    }

    public static void setup(@NotNull ContentEntryWrapper wrapper, @NotNull VirtualFile contentRoot) {
        for (String dirName : CargoConstants.ProjectLayout.sources) {
            VirtualFile child = contentRoot.findChild(dirName);
            if (child != null) {
                wrapper.addSourceFolder(child.getUrl(), false);
            }
        }
        for (String dirName : CargoConstants.ProjectLayout.tests) {
            VirtualFile child = contentRoot.findChild(dirName);
            if (child != null) {
                wrapper.addSourceFolder(child.getUrl(), true);
            }
        }
        VirtualFile targetChild = contentRoot.findChild(CargoConstants.ProjectLayout.target);
        if (targetChild != null) {
            wrapper.addExcludeFolder(targetChild.getUrl());
        }
    }

    public static boolean isNewProjectModelImportEnabled() {
        return Registry.is("org.rust.cargo.new.auto.import", false);
    }
}
