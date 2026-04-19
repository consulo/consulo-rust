/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.ide.notifications;

import com.intellij.ide.impl.TrustedProjects;
import com.intellij.ide.scratch.ScratchUtil;
import com.intellij.openapi.fileEditor.FileEditor;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.rust.RsBundle;
import org.rust.cargo.project.model.*;
import org.rust.lang.core.psi.RsFile;
import org.rust.openapiext.OpenApiUtil;
import org.rust.cargo.project.model.AttachCargoProjectAction;
import org.rust.cargo.project.model.CargoProjectServiceUtil;

public class NoCargoProjectNotificationProvider extends RsNotificationProvider {

    private static final String NOTIFICATION_STATUS_KEY = "org.rust.hideNoCargoProjectNotifications";

    public static final String NO_CARGO_PROJECTS = "NoCargoProjects";
    public static final String FILE_NOT_IN_CARGO_PROJECT = "FileNotInCargoProject";

    public NoCargoProjectNotificationProvider(@NotNull Project project) {
        super(project);

        project.getMessageBus().connect().subscribe(
            CargoProjectsService.CARGO_PROJECTS_TOPIC,
            (CargoProjectsService.CargoProjectsListener) (projects, reason) -> updateAllNotifications()
        );
    }

    @NotNull
    @Override
    protected String getDisablingKey(@NotNull VirtualFile file) {
        return NOTIFICATION_STATUS_KEY + file.getPath();
    }

    @Nullable
    @Override
    protected RsEditorNotificationPanel createNotificationPanel(@NotNull VirtualFile file, @NotNull FileEditor editor, @NotNull Project project) {
        if (OpenApiUtil.isUnitTestMode() && !OpenApiUtil.isDispatchThread()) return null;
        if (!(RsFile.isRustFile(file) || AttachCargoProjectAction.isCargoToml(file)) || isNotificationDisabled(file)) return null;
        if (ScratchUtil.isScratch(file)) return null;
        if (!TrustedProjects.isTrusted(project)) return null;

        CargoProjectsService cargoProjects = CargoProjectServiceUtil.getCargoProjects(project);
        if (!cargoProjects.getInitialized()) return null;
        if (!cargoProjects.getHasAtLeastOneValidProject()) {
            return createNoCargoProjectsPanel(file);
        }

        if (AttachCargoProjectAction.isCargoToml(file)) {
            if (AttachCargoProjectAction.canBeAttached(project, file)) {
                return createNoCargoProjectForFilePanel(file);
            }
        } else if (cargoProjects.findProjectForFile(file) == null) {
            return createNoCargoProjectForFilePanel(file);
        }

        return null;
    }

    @NotNull
    private RsEditorNotificationPanel createNoCargoProjectsPanel(@NotNull VirtualFile file) {
        return createAttachCargoProjectPanel(NO_CARGO_PROJECTS, file, RsBundle.message("notification.no.cargo.projects.found"));
    }

    @NotNull
    private RsEditorNotificationPanel createNoCargoProjectForFilePanel(@NotNull VirtualFile file) {
        return createAttachCargoProjectPanel(FILE_NOT_IN_CARGO_PROJECT, file, RsBundle.message("notification.file.not.belong.to.cargo.project"));
    }

    @NotNull
    private RsEditorNotificationPanel createAttachCargoProjectPanel(@NotNull String debugId, @NotNull VirtualFile file, @NotNull String message) {
        RsEditorNotificationPanel panel = new RsEditorNotificationPanel(debugId);
        panel.setText(message);
        panel.createActionLabel(RsBundle.message("notification.action.attach.text"), "Cargo.AttachCargoProject");
        panel.createActionLabel(RsBundle.message("notification.action.do.not.show.again.text"), () -> {
            disableNotification(file);
            updateAllNotifications();
        });
        return panel;
    }
}
