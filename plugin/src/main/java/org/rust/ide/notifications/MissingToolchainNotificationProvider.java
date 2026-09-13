/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.ide.notifications;

import org.rust.cargo.project.workspace.StandardLibraryFactory;

import org.rust.cargo.api.model.CargoProject;
import org.rust.cargo.api.model.CargoProjectsService;
import org.rust.cargo.api.model.RustcInfo;

import org.rust.cargo.toolchain.RsToolchainLocator;
import consulo.project.ui.notification.NotificationType;
import consulo.fileChooser.FileChooser;
import consulo.fileChooser.FileChooserDescriptorFactory;
import consulo.fileEditor.EditorNotificationBuilder;
import consulo.fileEditor.FileEditor;
import consulo.application.dumb.DumbAware;
import consulo.project.Project;
import consulo.virtualFileSystem.VirtualFile;
import consulo.annotation.component.ExtensionImpl;
import jakarta.annotation.Nonnull;
import jakarta.inject.Inject;
import jakarta.annotation.Nullable;
import org.rust.RsBundle;
import org.rust.cargo.project.model.*;
import org.rust.cargo.api.settings.RsProjectSettingsServiceUtil;
import org.rust.cargo.api.workspace.StandardLibrary;
import org.rust.cargo.toolchain.RsToolchainBase;
import org.rust.cargo.toolchain.tools.Rustup;
import org.rust.lang.core.psi.impl.RsFile;
import org.rust.openapiext.OpenApiUtil;
import org.rust.cargo.project.model.AttachCargoProjectAction;
import org.rust.cargo.project.model.CargoProjectServiceUtil;
import org.rust.cargo.api.model.CargoProjectsListener;
import org.rust.cargo.api.settings.RsSettingsListener;

import java.util.function.Supplier;
import org.rust.cargo.api.settings.RsProjectSettingsServiceBase;
import consulo.ide.setting.ShowSettingsUtil;
import org.rust.cargo.project.configurable.RsProjectConfigurable;
import org.rust.notifications.NotificationUtils;
import org.rust.notifications.RsEditorNotificationPanel;
import org.rust.notifications.RsNotificationProvider;

@ExtensionImpl
public class MissingToolchainNotificationProvider extends RsNotificationProvider implements DumbAware {

    private static final String NOTIFICATION_STATUS_KEY = "org.rust.hideToolchainNotifications";
    public static final String NO_RUST_TOOLCHAIN = "NoRustToolchain";
    public static final String NO_ATTACHED_STDLIB = "NoAttachedStdlib";

    @Inject
    public MissingToolchainNotificationProvider(@Nonnull Project project) {
        super(project);

        project.getMessageBus().connect().subscribe(
            org.rust.cargo.api.settings.RsProjectSettingsServiceBase.RUST_SETTINGS_TOPIC,
            new RsSettingsListener() {
                @Override
                public void settingsChanged(
                    @Nonnull RsProjectSettingsServiceBase.SettingsChangedEventBase<?> e
                ) {
                    updateAllNotifications();
                }
            }
        );

        project.getMessageBus().connect().subscribe(
            CargoProjectsService.CARGO_PROJECTS_TOPIC,
            (CargoProjectsListener) (projects, reason) -> updateAllNotifications()
        );
    }

    @Nonnull
    @Override
    protected String getDisablingKey(@Nonnull VirtualFile file) {
        return NOTIFICATION_STATUS_KEY;
    }

    @Nullable
    @Override
    protected RsEditorNotificationPanel createNotificationPanel(
        @Nonnull VirtualFile file,
        @Nonnull FileEditor editor,
        @Nonnull Project project,
        @Nonnull Supplier<EditorNotificationBuilder> builderFactory
    ) {
        if (OpenApiUtil.isUnitTestMode()) return null;
        if (!(RsFile.isRustFile(file) || AttachCargoProjectAction.isCargoToml(file)) || isNotificationDisabled(file)) return null;
        if (CargoProjectServiceUtil.guessAndSetupRustProject(project)) return null;

        RsToolchainBase toolchain = RsToolchainLocator.getToolchain(project);
        if (toolchain == null || !toolchain.looksLikeValidToolchain()) {
            return createBadToolchainPanel(file, builderFactory);
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
            return createLibraryAttachingPanel(project, file, rustcInfo, builderFactory);
        }

        return null;
    }

    @Nonnull
    private RsEditorNotificationPanel createBadToolchainPanel(
        @Nonnull VirtualFile file,
        @Nonnull Supplier<EditorNotificationBuilder> builderFactory
    ) {
        RsEditorNotificationPanel panel = new RsEditorNotificationPanel(NO_RUST_TOOLCHAIN, builderFactory.get());
        panel.setText(RsBundle.message("notification.no.toolchain.configured"));
        panel.createActionLabel(RsBundle.message("notification.action.set.up.toolchain.text"), () -> {
            ShowSettingsUtil.getInstance().showSettingsDialog(myProject, RsProjectConfigurable.class);
        });
        panel.createActionLabel(RsBundle.message("notification.action.do.not.show.again.text"), () -> {
            disableNotification(file);
            updateAllNotifications();
        });
        return panel;
    }

    @Nonnull
    private RsEditorNotificationPanel createLibraryAttachingPanel(
        @Nonnull Project project,
        @Nonnull VirtualFile file,
        @Nullable RustcInfo rustcInfo,
        @Nonnull Supplier<EditorNotificationBuilder> builderFactory
    ) {
        RsEditorNotificationPanel panel = new RsEditorNotificationPanel(NO_ATTACHED_STDLIB, builderFactory.get());
        panel.setText(RsBundle.message("notification.can.not.attach.stdlib.sources"));
        panel.createActionLabel(RsBundle.message("notification.action.attach.manually.text"), () -> {
            FileChooser.chooseFile(
                FileChooserDescriptorFactory.createSingleFolderDescriptor(),
                myProject, null
            ).whenComplete((stdlib, throwable) -> {
                if (throwable != null || stdlib == null) return;
                if (StandardLibraryFactory.fromFile(project, stdlib, rustcInfo) != null) {
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
