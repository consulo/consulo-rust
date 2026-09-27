/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.cargo.project.model.impl;

import consulo.disposer.Disposable;
import consulo.document.FileDocumentManager;
import consulo.externalSystem.autoimport.ExternalSystemModificationType;
import consulo.externalSystem.autoimport.ExternalSystemProjectAware;
import consulo.externalSystem.autoimport.ExternalSystemProjectId;
import consulo.externalSystem.autoimport.ExternalSystemProjectListener;
import consulo.externalSystem.autoimport.ExternalSystemProjectReloadContext;
import consulo.externalSystem.autoimport.ExternalSystemRefreshStatus;
import consulo.externalSystem.autoimport.ExternalSystemSettingsFilesModificationContext;
import consulo.externalSystem.autoimport.ExternalSystemSettingsFilesModificationContext.ReloadStatus;
import consulo.externalSystem.model.ProjectSystemId;
import consulo.localize.LocalizeValue;
import consulo.project.Project;
import consulo.rust.icon.RustIconGroup;
import consulo.util.io.PathUtil;
import jakarta.annotation.Nonnull;
import org.rust.cargo.CargoConstants;
import org.rust.cargo.api.model.CargoProjectsRefreshListener;
import org.rust.cargo.api.model.CargoProjectsService;
import org.rust.cargo.api.model.CargoProjectsService.CargoRefreshStatus;
import org.rust.cargo.project.model.CargoProjectServiceUtil;

import java.util.Map;
import java.util.Set;

public class CargoExternalSystemProjectAware implements ExternalSystemProjectAware {

    public static final ProjectSystemId CARGO_SYSTEM_ID = new ProjectSystemId("Cargo", LocalizeValue.localizeTODO("Cargo"), RustIconGroup.cargo(), RustIconGroup.cargo());

    private final Project project;
    private final ExternalSystemProjectId projectId;

    public CargoExternalSystemProjectAware(@Nonnull Project project) {
        this.project = project;
        this.projectId = new ExternalSystemProjectId(CARGO_SYSTEM_ID, project.getName());
    }

    @Nonnull
    @Override
    public ExternalSystemProjectId getProjectId() {
        return projectId;
    }

    @Nonnull
    @Override
    public Set<String> getSettingsFiles() {
        CargoSettingsFilesService settingsFilesService = CargoSettingsFilesService.getInstance(project);
        return settingsFilesService.collectSettingsFiles(false).keySet();
    }

    @Override
    public boolean isIgnoredSettingsFileEvent(@Nonnull String path, @Nonnull ExternalSystemSettingsFilesModificationContext context) {
        if (ExternalSystemProjectAware.super.isIgnoredSettingsFileEvent(path, context)) return true;

        String fileName = PathUtil.getFileName(path);
        if (CargoConstants.LOCK_FILE.equals(fileName) &&
            context.getModificationType() == ExternalSystemModificationType.EXTERNAL &&
            (context.getReloadStatus() == ReloadStatus.IN_PROGRESS || context.getReloadStatus() == ReloadStatus.JUST_FINISHED)) {
            return true;
        }

        if (context.getEvent() != ExternalSystemSettingsFilesModificationContext.Event.UPDATE) return false;

        Map<String, CargoSettingsFilesService.SettingFileType> settingsFiles =
            CargoSettingsFilesService.getInstance(project).collectSettingsFiles(true);
        CargoSettingsFilesService.SettingFileType settingFileType = settingsFiles.get(path);
        return settingFileType == null || settingFileType == CargoSettingsFilesService.SettingFileType.IMPLICIT_TARGET;
    }

    @Override
    public void reloadProject(@Nonnull ExternalSystemProjectReloadContext context) {
        FileDocumentManager.getInstance().saveAllDocuments();
        CargoProjectServiceUtil.getCargoProjects(project).refreshAllProjects();
    }

    @Override
    public boolean isDisabledReload(@Nonnull ExternalSystemProjectReloadContext context) {
        return CargoProjectServiceUtil.getCargoProjects(project).getAllProjects().isEmpty();
    }

    @Override
    public void subscribe(@Nonnull ExternalSystemProjectListener listener, @Nonnull Disposable parentDisposable) {
        project.getMessageBus().connect(parentDisposable).subscribe(
            CargoProjectsService.CARGO_PROJECTS_REFRESH_TOPIC,
            new CargoProjectsRefreshListener() {
                @Override
                public void onRefreshStarted() {
                    listener.onProjectReloadStart();
                }

                @Override
                public void onRefreshFinished(@Nonnull CargoRefreshStatus status) {
                    ExternalSystemRefreshStatus externalStatus = switch (status) {
                        case SUCCESS -> ExternalSystemRefreshStatus.SUCCESS;
                        case FAILURE -> ExternalSystemRefreshStatus.FAILURE;
                        case CANCEL -> ExternalSystemRefreshStatus.CANCEL;
                    };
                    listener.onProjectReloadFinish(externalStatus);
                }
            }
        );
    }
}
