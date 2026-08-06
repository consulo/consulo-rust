/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.ide.notifications;

import com.intellij.ide.impl.TrustedProjects;
import consulo.project.ui.notification.NotificationType;
import consulo.fileChooser.FileChooser;
import consulo.fileChooser.FileChooserDescriptorFactory;
import consulo.fileEditor.FileEditor;
import consulo.application.dumb.DumbAware;
import consulo.project.Project;
import consulo.virtualFileSystem.VirtualFile;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import org.rust.RsBundle;
import org.rust.cargo.project.model.*;
import org.rust.cargo.project.settings.RsProjectSettingsServiceUtil;
import org.rust.cargo.project.workspace.StandardLibrary;
import org.rust.cargo.toolchain.RsToolchainBase;
import org.rust.cargo.toolchain.tools.Rustup;
import org.rust.lang.core.psi.RsFile;
import org.rust.openapiext.OpenApiUtil;
import org.rust.cargo.project.model.AttachCargoProjectAction;
import org.rust.cargo.project.model.CargoProjectServiceUtil;

public class MissingToolchainNotificationProvider extends RsNotificationProvider implements DumbAware {

    private static final String NOTIFICATION_STATUS_KEY = "org.rust.hideToolchainNotifications";
    public static final String NO_RUST_TOOLCHAIN = "NoRustToolchain";
    public static final String NO_ATTACHED_STDLIB = "NoAttachedStdlib";

    public MissingToolchainNotificationProvider(@Nonnull Project project) {
        super(project);

        project.getMessageBus().connect().subscribe(
            org.rust.cargo.project.settings.RsProjectSettingsServiceBase.RUST_SETTINGS_TOPIC,
            new org.rust.cargo.project.settings.RsProjectSettingsServiceBase.RsSettingsListener() {
                @Override
                public <T extends org.rust.cargo.project.settings.RsProjectSettingsServiceBase.RsProjectSettingsBase<T>> void settingsChanged(
                    @Nonnull org.rust.cargo.project.settings.RsProjectSettingsServiceBase.SettingsChangedEventBase<T> e
                ) {
                    updateAllNotifications();
                }
            }
        );

        project.getMessageBus().connect().subscribe(
            CargoProjectsService.CARGO_PROJECTS_TOPIC,
            (CargoProjectsService.CargoProjectsListener) (projects, reason) -> updateAllNotifications()
        );
    }

    @Nonnull
    @Override
    protected String getDisablingKey(@Nonnull VirtualFile file) {
        return NOTIFICATION_STATUS_KEY;
    }

    @Nullable
    @Override
    protected RsEditorNotificationPanel createNotificationPanel(@Nonnull VirtualFile file, @Nonnull FileEditor editor, @Nonnull Project project) {
        if (OpenApiUtil.isUnitTestMode()) return null;
        if (!(RsFile.isRustFile(file) || AttachCargoProjectAction.isCargoToml(file)) || isNotificationDisabled(file)) return null;
        if (!TrustedProjects.isTrusted(project)) return null;
        if (CargoProjectServiceUtil.guessAndSetupRustProject(project)) return null;

        RsToolchainBase toolchain = RsProjectSettingsServiceUtil.getToolchain(project);
        if (toolchain == null || !toolchain.looksLikeValidToolchain()) {
            return createBadToolchainPanel(file);
        }

        CargoProjectsService cargoProjects = CargoProjectServiceUtil.getCargoProjects(project);
        if (!cargoProjects.getInitialized()) return null;

        CargoProject cargoProject = cargoProjects.findProjectForFile(file);
        if (cargoProject == null) return null;
        Object workspace = cargoProject.getWorkspace();
        if (workspace == null) return null;
        // Check for standard library
        if (!Rustup.isRustupAvailable(toolchain)) {
            RustcInfo rustcInfo = cargoProject.getRustcInfo();
            return createLibraryAttachingPanel(project, file, rustcInfo);
        }

        return null;
    }

    @Nonnull
    private RsEditorNotificationPanel createBadToolchainPanel(@Nonnull VirtualFile file) {
        RsEditorNotificationPanel panel = new RsEditorNotificationPanel(NO_RUST_TOOLCHAIN);
        panel.setText(RsBundle.message("notification.no.toolchain.configured"));
        panel.createActionLabel(RsBundle.message("notification.action.set.up.toolchain.text"), () -> {
            RsProjectSettingsServiceUtil.getRustSettings(myProject).configureToolchain();
        });
        panel.createActionLabel(RsBundle.message("notification.action.do.not.show.again.text"), () -> {
            disableNotification(file);
            updateAllNotifications();
        });
        return panel;
    }

    @Nonnull
    private RsEditorNotificationPanel createLibraryAttachingPanel(@Nonnull Project project, @Nonnull VirtualFile file, @Nullable RustcInfo rustcInfo) {
        RsEditorNotificationPanel panel = new RsEditorNotificationPanel(NO_ATTACHED_STDLIB);
        panel.setText(RsBundle.message("notification.can.not.attach.stdlib.sources"));
        panel.createActionLabel(RsBundle.message("notification.action.attach.manually.text"), () -> {
            FileChooser.chooseFile(
                FileChooserDescriptorFactory.createSingleFolderDescriptor(),
                panel, myProject, null
            ).doWhenDone((VirtualFile stdlib) -> {
                if (StandardLibrary.fromFile(project, stdlib, rustcInfo) != null) {
                    RsProjectSettingsServiceUtil.getRustSettings(myProject).modify(it -> {
                        it.explicitPathToStdlib = stdlib.getPath();
                    });
                } else {
                    NotificationUtils.showBalloon(myProject,
                        RsBundle.message("notification.invalid.stdlib.source.path", stdlib.getPresentableUrl()),
                        NotificationType.ERROR);
                }
                updateAllNotifications();
            });
        });
        panel.createActionLabel(RsBundle.message("notification.action.do.not.show.again.text"), () -> {
            disableNotification(file);
            updateAllNotifications();
        });
        return panel;
    }
}
