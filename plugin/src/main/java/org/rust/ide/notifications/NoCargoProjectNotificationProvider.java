/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.ide.notifications;

import consulo.language.editor.scratch.ScratchUtil;
import consulo.fileEditor.EditorNotificationBuilder;
import consulo.fileEditor.FileEditor;
import consulo.project.Project;
import consulo.virtualFileSystem.VirtualFile;
import consulo.annotation.component.ExtensionImpl;
import jakarta.annotation.Nonnull;
import jakarta.inject.Inject;
import jakarta.annotation.Nullable;
import org.rust.RsBundle;
import org.rust.cargo.project.model.*;
import org.rust.lang.core.psi.RsFile;
import org.rust.openapiext.OpenApiUtil;
import org.rust.cargo.project.model.AttachCargoProjectAction;
import org.rust.cargo.project.model.CargoProjectServiceUtil;
import org.rust.cargo.project.model.CargoProjectsListener;

import java.util.function.Supplier;

@ExtensionImpl
public class NoCargoProjectNotificationProvider extends RsNotificationProvider {

    private static final String NOTIFICATION_STATUS_KEY = "org.rust.hideNoCargoProjectNotifications";

    public static final String NO_CARGO_PROJECTS = "NoCargoProjects";
    public static final String FILE_NOT_IN_CARGO_PROJECT = "FileNotInCargoProject";

    @Inject
    public NoCargoProjectNotificationProvider(@Nonnull Project project) {
        super(project);

        project.getMessageBus().connect().subscribe(
            CargoProjectsService.CARGO_PROJECTS_TOPIC,
            (CargoProjectsListener) (projects, reason) -> updateAllNotifications()
        );
    }

    @Nonnull
    @Override
    protected String getDisablingKey(@Nonnull VirtualFile file) {
        return NOTIFICATION_STATUS_KEY + file.getPath();
    }

    @Nullable
    @Override
    protected RsEditorNotificationPanel createNotificationPanel(
        @Nonnull VirtualFile file,
        @Nonnull FileEditor editor,
        @Nonnull Project project,
        @Nonnull Supplier<EditorNotificationBuilder> builderFactory
    ) {
        if (OpenApiUtil.isUnitTestMode() && !OpenApiUtil.isDispatchThread()) return null;
        if (!(RsFile.isRustFile(file) || AttachCargoProjectAction.isCargoToml(file)) || isNotificationDisabled(file)) return null;
        if (ScratchUtil.isScratch(file)) return null;
        // TrustedProjects gating isn't wired in the Consulo port; assume trusted

        CargoProjectsService cargoProjects = CargoProjectServiceUtil.getCargoProjects(project);
        if (!cargoProjects.getInitialized()) return null;
        if (!cargoProjects.getHasAtLeastOneValidProject()) {
            return createNoCargoProjectsPanel(file, builderFactory);
        }

        if (AttachCargoProjectAction.isCargoToml(file)) {
            if (AttachCargoProjectAction.canBeAttached(project, file)) {
                return createNoCargoProjectForFilePanel(file, builderFactory);
            }
        } else if (cargoProjects.findProjectForFile(file) == null) {
            return createNoCargoProjectForFilePanel(file, builderFactory);
        }

        return null;
    }

    @Nonnull
    private RsEditorNotificationPanel createNoCargoProjectsPanel(
        @Nonnull VirtualFile file,
        @Nonnull Supplier<EditorNotificationBuilder> builderFactory
    ) {
        return createAttachCargoProjectPanel(NO_CARGO_PROJECTS, file,
            RsBundle.message("notification.no.cargo.projects.found"), builderFactory);
    }

    @Nonnull
    private RsEditorNotificationPanel createNoCargoProjectForFilePanel(
        @Nonnull VirtualFile file,
        @Nonnull Supplier<EditorNotificationBuilder> builderFactory
    ) {
        return createAttachCargoProjectPanel(FILE_NOT_IN_CARGO_PROJECT, file,
            RsBundle.message("notification.file.not.belong.to.cargo.project"), builderFactory);
    }

    @Nonnull
    private RsEditorNotificationPanel createAttachCargoProjectPanel(
        @Nonnull String debugId,
        @Nonnull VirtualFile file,
        @Nonnull String message,
        @Nonnull Supplier<EditorNotificationBuilder> builderFactory
    ) {
        RsEditorNotificationPanel panel = new RsEditorNotificationPanel(debugId, builderFactory.get());
        panel.setText(message);
        panel.createActionLabel(RsBundle.message("notification.action.attach.text"), "Cargo.AttachCargoProject", myProject, file);
        panel.createActionLabel(RsBundle.message("notification.action.do.not.show.again.text"), () -> {
            disableNotification(file);
            updateAllNotifications();
        });
        return panel;
    }
}
