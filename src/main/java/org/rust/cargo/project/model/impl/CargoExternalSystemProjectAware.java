/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.cargo.project.model.impl;

import com.intellij.openapi.Disposable;
import com.intellij.openapi.externalSystem.autoimport.*;
import com.intellij.openapi.externalSystem.autoimport.ExternalSystemSettingsFilesModificationContext.ReloadStatus;
import com.intellij.openapi.externalSystem.model.ProjectSystemId;
import com.intellij.openapi.fileEditor.FileDocumentManager;
import com.intellij.openapi.project.Project;
import com.intellij.util.PathUtil;
import org.jetbrains.annotations.NotNull;
import org.rust.cargo.CargoConstants;
import org.rust.cargo.project.model.CargoProjectServiceUtil;
import org.rust.cargo.project.model.CargoProjectsService;
import org.rust.cargo.project.model.CargoProjectsService.CargoRefreshStatus;

import java.util.Map;
import java.util.Set;

@SuppressWarnings("UnstableApiUsage")
public class CargoExternalSystemProjectAware implements ExternalSystemProjectAware {

    public static final ProjectSystemId CARGO_SYSTEM_ID = new ProjectSystemId("Cargo");

    private final Project project;
    private final ExternalSystemProjectId projectId;

    public CargoExternalSystemProjectAware(@NotNull Project project) {
        this.project = project;
        this.projectId = new ExternalSystemProjectId(CARGO_SYSTEM_ID, project.getName());
    }

    @NotNull
    @Override
    public ExternalSystemProjectId getProjectId() {
        return projectId;
    }

    @NotNull
    @Override
    public Set<String> getSettingsFiles() {
        CargoSettingsFilesService settingsFilesService = CargoSettingsFilesService.getInstance(project);
        return settingsFilesService.collectSettingsFiles(false).keySet();
    }

    @Override
    public boolean isIgnoredSettingsFileEvent(@NotNull String path, @NotNull ExternalSystemSettingsFilesModificationContext context) {
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
    public void reloadProject(@NotNull ExternalSystemProjectReloadContext context) {
        FileDocumentManager.getInstance().saveAllDocuments();
        CargoProjectServiceUtil.getCargoProjects(project).refreshAllProjects();
    }

    @Override
    public void subscribe(@NotNull ExternalSystemProjectListener listener, @NotNull Disposable parentDisposable) {
        project.getMessageBus().connect(parentDisposable).subscribe(
            CargoProjectsService.CARGO_PROJECTS_REFRESH_TOPIC,
            new CargoProjectsService.CargoProjectsRefreshListener() {
                @Override
                public void onRefreshStarted() {
                    listener.onProjectReloadStart();
                }

                @Override
                public void onRefreshFinished(@NotNull CargoRefreshStatus status) {
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
