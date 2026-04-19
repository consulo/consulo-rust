/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.cargo.project.model;

import com.google.common.annotations.VisibleForTesting;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.DataKey;
import com.intellij.openapi.actionSystem.PlatformDataKeys;
import com.intellij.openapi.fileChooser.FileChooserDescriptor;
import com.intellij.openapi.fileChooser.FileChooserFactory;
import com.intellij.openapi.project.DumbService;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.roots.ProjectFileIndex;
import com.intellij.openapi.ui.Messages;
import com.intellij.openapi.vfs.VirtualFile;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.rust.RsBundle;
import org.rust.cargo.CargoConstants;
import org.rust.cargo.project.toolwindow.CargoToolWindow;
import org.rust.ide.notifications.RsEditorNotificationPanel;
import org.rust.openapiext.OpenApiUtil;

import java.nio.file.Path;

/**
 * Adds cargo project to {@link CargoProjectsService}.
 *
 * It can be invoked from Project View, {@link CargoToolWindow} and {@link RsEditorNotificationPanel}
 */
public class AttachCargoProjectAction extends CargoProjectActionBase {

    @VisibleForTesting
    public static final DataKey<VirtualFile> MOCK_CHOSEN_FILE_KEY = DataKey.create("MOCK_CHOSEN_FILE_KEY");

    @Override
    public void actionPerformed(@NotNull AnActionEvent e) {
        Project project = e.getProject();
        if (project == null) return;
        OpenApiUtil.saveAllDocuments();

        VirtualFile file;
        switch (e.getPlace()) {
            case CargoToolWindow.CARGO_TOOLBAR_PLACE:
                file = chooseFile(project, e);
                break;
            case RsEditorNotificationPanel.NOTIFICATION_PANEL_PLACE: {
                VirtualFile dataFile = e.getData(PlatformDataKeys.VIRTUAL_FILE);
                file = (dataFile != null && isCargoToml(dataFile)) ? dataFile : chooseFile(project, e);
                break;
            }
            default:
                file = e.getData(PlatformDataKeys.VIRTUAL_FILE);
                break;
        }

        if (file == null) return;

        VirtualFile cargoToml = findCargoToml(file);
        if (cargoToml == null) return;

        if (!CargoProjectServiceUtil.getCargoProjects(project).attachCargoProject(OpenApiUtil.getPathAsPath(cargoToml))) {
            Messages.showErrorDialog(
                project,
                RsBundle.message("dialog.message.this.cargo.package.already.part.attached.workspace"),
                RsBundle.message("dialog.title.unable.to.attach.cargo.project")
            );
        }
    }

    @Nullable
    private VirtualFile chooseFile(@NotNull Project project, @NotNull AnActionEvent event) {
        if (OpenApiUtil.isUnitTestMode()) {
            return event.getData(MOCK_CHOSEN_FILE_KEY);
        } else {
            var chooser = FileChooserFactory.getInstance().createFileChooser(CargoProjectChooserDescriptor.INSTANCE, project, null);
            VirtualFile[] chosen = chooser.choose(project);
            return chosen.length == 1 ? chosen[0] : null;
        }
    }

    @Override
    public void update(@NotNull AnActionEvent e) {
        Project project = e.getProject();
        if (project == null) return;
        e.getPresentation().setEnabledAndVisible(isActionEnabled(e, project));
    }

    private boolean isActionEnabled(@NotNull AnActionEvent e, @NotNull Project project) {
        switch (e.getPlace()) {
            case CargoToolWindow.CARGO_TOOLBAR_PLACE:
            case RsEditorNotificationPanel.NOTIFICATION_PANEL_PLACE:
                return true;
            default:
                if (DumbService.isDumb(project)) return false;
                VirtualFile file = e.getData(PlatformDataKeys.VIRTUAL_FILE);
                VirtualFile cargoToml = file != null ? findCargoToml(file) : null;
                if (cargoToml == null) return false;
                return canBeAttached(project, cargoToml);
        }
    }

    @Nullable
    private static VirtualFile findCargoToml(@NotNull VirtualFile file) {
        if (file.isDirectory()) {
            return file.findChild(CargoConstants.MANIFEST_FILE);
        } else {
            return isCargoToml(file) ? file : null;
        }
    }

    public static boolean canBeAttached(@NotNull Project project, @NotNull VirtualFile cargoToml) {
        if (!isCargoToml(cargoToml)) throw new IllegalArgumentException("Not a Cargo.toml file");
        if (!ProjectFileIndex.getInstance(project).isInContent(cargoToml)) return false;

        Path path = OpenApiUtil.getPathAsPath(cargoToml);

        CargoProjectsService service = CargoProjectServiceUtil.getCargoProjects(project);
        for (CargoProject cp : service.getAllProjects()) {
            if (cp.getManifest().equals(path)) return false;
        }
        for (CargoProject cp : service.getAllProjects()) {
            if (containsWorkspaceManifest(cp, path)) return false;
        }
        return true;
    }

    private static boolean containsWorkspaceManifest(@NotNull CargoProject cargoProject, @NotNull Path path) {
        Path rootDir = path.getParent();
        var workspace = cargoProject.getWorkspace();
        if (workspace == null) return false;
        for (var pkg : workspace.getPackages()) {
            if (pkg.getRootDirectory().equals(rootDir)) return true;
        }
        return false;
    }

    public static boolean isCargoToml(@NotNull VirtualFile file) {
        return file.getName().equals(CargoConstants.MANIFEST_FILE);
    }
}
