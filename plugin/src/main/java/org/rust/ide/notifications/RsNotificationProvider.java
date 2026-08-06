/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.ide.notifications;

import consulo.component.PropertiesComponent;
import consulo.fileEditor.FileEditor;
import consulo.project.Project;
import consulo.virtualFileSystem.VirtualFile;
import consulo.fileEditor.EditorNotificationProvider;
import consulo.fileEditor.EditorNotifications;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

import java.util.function.Function;

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
    public consulo.fileEditor.EditorNotificationBuilder buildNotification(
        @Nonnull VirtualFile file,
        @Nonnull FileEditor fileEditor,
        @Nonnull java.util.function.Supplier<consulo.fileEditor.EditorNotificationBuilder> builderFactory
    ) {
        return null;
    }

    @Nonnull
    protected abstract String getDisablingKey(@Nonnull VirtualFile file);

    @Nonnull
    public final Function<FileEditor, RsEditorNotificationPanel> collectNotificationData(@Nonnull Project project, @Nonnull VirtualFile file) {
        return editor -> createNotificationPanel(file, editor, project);
    }

    @Nullable
    protected abstract RsEditorNotificationPanel createNotificationPanel(
        @Nonnull VirtualFile file,
        @Nonnull FileEditor editor,
        @Nonnull Project project
    );

    protected void updateAllNotifications() {
        EditorNotifications.getInstance(myProject).updateAllNotifications();
    }

    protected void disableNotification(@Nonnull VirtualFile file) {
        myProject.getInstance(consulo.component.PropertiesComponent.class).setValue(getDisablingKey(file), true);
    }

    protected boolean isNotificationDisabled(@Nonnull VirtualFile file) {
        return myProject.getInstance(consulo.component.PropertiesComponent.class).getBoolean(getDisablingKey(file));
    }
}
