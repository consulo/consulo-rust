/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.cargo.project.model;

import consulo.ui.ex.action.AnActionEvent;
import consulo.util.dataholder.Key;
import consulo.language.editor.PlatformDataKeys;
import consulo.fileChooser.FileChooserDescriptor;
import consulo.fileChooser.FileChooserFactory;
import consulo.project.DumbService;
import consulo.project.Project;
import consulo.module.content.ProjectFileIndex;
import consulo.ui.ex.awt.Messages;
import consulo.virtualFileSystem.VirtualFile;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import org.rust.RsBundle;
import org.rust.cargo.CargoConstants;
import org.rust.cargo.project.toolwindow.CargoToolWindow;
import org.rust.ide.notifications.RsEditorNotificationPanel;
import org.rust.openapiext.OpenApiUtil;

import java.nio.file.Path;
import consulo.annotation.component.ActionImpl;

/**
 * Adds cargo project to {@link CargoProjectsService}.
 *
 * It can be invoked from Project View, {@link CargoToolWindow} and {@link RsEditorNotificationPanel}
 */
@ActionImpl(id = "Cargo.AttachCargoProject")
public class AttachCargoProjectAction extends CargoProjectActionBase {

    
    public static final Key<VirtualFile> MOCK_CHOSEN_FILE_KEY = Key.create("MOCK_CHOSEN_FILE_KEY");

    @Override
    public void actionPerformed(@Nonnull AnActionEvent e) {
        Project project = e.getData(Project.KEY);
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
    private VirtualFile chooseFile(@Nonnull Project project, @Nonnull AnActionEvent event) {
        if (OpenApiUtil.isUnitTestMode()) {
            return event.getData(MOCK_CHOSEN_FILE_KEY);
        } else {
            var chooser = FileChooserFactory.getInstance().createFileChooser(CargoProjectChooserDescriptor.INSTANCE, project, null);
            VirtualFile[] chosen = chooser.choose(project);
            return chosen.length == 1 ? chosen[0] : null;
        }
    }

    @Override
    public void update(@Nonnull AnActionEvent e) {
        Project project = e.getData(Project.KEY);
        if (project == null) return;
        e.getPresentation().setEnabledAndVisible(isActionEnabled(e, project));
    }

    private boolean isActionEnabled(@Nonnull AnActionEvent e, @Nonnull Project project) {
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
    private static VirtualFile findCargoToml(@Nonnull VirtualFile file) {
        if (file.isDirectory()) {
            return file.findChild(CargoConstants.MANIFEST_FILE);
        } else {
            return isCargoToml(file) ? file : null;
        }
    }

    public static boolean canBeAttached(@Nonnull Project project, @Nonnull VirtualFile cargoToml) {
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

    private static boolean containsWorkspaceManifest(@Nonnull CargoProject cargoProject, @Nonnull Path path) {
        Path rootDir = path.getParent();
        var workspace = cargoProject.getWorkspace();
        if (workspace == null) return false;
        for (var pkg : workspace.getPackages()) {
            if (pkg.getRootDirectory().equals(rootDir)) return true;
        }
        return false;
    }

    public static boolean isCargoToml(@Nonnull VirtualFile file) {
        return file.getName().equals(CargoConstants.MANIFEST_FILE);
    }
}
