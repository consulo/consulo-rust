/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.ide.notifications;

import consulo.fileEditor.EditorNotificationBuilder;
import consulo.fileEditor.EditorNotificationProvider;
import consulo.fileEditor.EditorNotifications;
import consulo.fileEditor.FileEditor;
import consulo.project.Project;
import consulo.project.ProjectPropertiesComponent;
import consulo.virtualFileSystem.VirtualFile;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

import java.util.function.Supplier;

public abstract class RsNotificationProvider implements EditorNotificationProvider {

    protected final Project myProject;

    protected RsNotificationProvider(@Nonnull Project project) {
        myProject = project;
    }

    @Nonnull
    @Override
    public String getId() {
        return getClass().getName();
    }

    @Nullable
    @Override
    public final EditorNotificationBuilder buildNotification(
        @Nonnull VirtualFile file,
        @Nonnull FileEditor fileEditor,
        @Nonnull Supplier<EditorNotificationBuilder> builderFactory
    ) {
        RsEditorNotificationPanel panel = createNotificationPanel(file, fileEditor, myProject, builderFactory);
        return panel == null ? null : panel.getBuilder();
    }

    @Nonnull
    protected abstract String getDisablingKey(@Nonnull VirtualFile file);

    @Nullable
    protected abstract RsEditorNotificationPanel createNotificationPanel(
        @Nonnull VirtualFile file,
        @Nonnull FileEditor editor,
        @Nonnull Project project,
        @Nonnull Supplier<EditorNotificationBuilder> builderFactory
    );

    protected void updateAllNotifications() {
        EditorNotifications.getInstance(myProject).updateAllNotifications();
    }

    protected void disableNotification(@Nonnull VirtualFile file) {
        ProjectPropertiesComponent.getInstance(myProject).setValue(getDisablingKey(file), true);
    }

    protected boolean isNotificationDisabled(@Nonnull VirtualFile file) {
        return ProjectPropertiesComponent.getInstance(myProject).getBoolean(getDisablingKey(file));
    }
}
