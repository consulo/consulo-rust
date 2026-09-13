/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.cargo.project.model.impl;
import com.intellij.openapi.externalSystem.service.project.autoimport.ExternalSystemRefreshStatus;
import com.intellij.openapi.externalSystem.model.ExternalSystemProjectId;

import consulo.disposer.Disposable;
import com.intellij.openapi.externalSystem.autoimport.*;
import com.intellij.openapi.externalSystem.autoimport.ExternalSystemSettingsFilesModificationContext.ReloadStatus;
import consulo.externalSystem.model.ProjectSystemId;
import consulo.document.FileDocumentManager;
import consulo.project.Project;
import consulo.util.io.PathUtil;
import jakarta.annotation.Nonnull;
import org.rust.cargo.CargoConstants;
import org.rust.cargo.project.model.CargoProjectServiceUtil;
import org.rust.cargo.api.model.CargoProjectsService;
import org.rust.cargo.api.model.CargoProjectsService.CargoRefreshStatus;

import java.util.Map;
import java.util.Set;
import org.rust.cargo.api.model.CargoProjectsRefreshListener;

@SuppressWarnings("UnstableApiUsage")
public class CargoExternalSystemProjectAware implements ExternalSystemProjectAware {

    public static final ProjectSystemId CARGO_SYSTEM_ID = new ProjectSystemId("Cargo");

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
                    listener.onProjectReloadFinish(externalStatus == ExternalSystemRefreshStatus.SUCCESS);
                }
            }
        );
    }
}
