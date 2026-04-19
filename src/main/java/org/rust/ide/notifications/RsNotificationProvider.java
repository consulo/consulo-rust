/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.ide.notifications;

import com.intellij.ide.util.PropertiesComponent;
import com.intellij.openapi.fileEditor.FileEditor;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.ui.EditorNotificationProvider;
import com.intellij.ui.EditorNotifications;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.function.Function;

public abstract class RsNotificationProvider implements EditorNotificationProvider {

    protected final Project myProject;

    protected RsNotificationProvider(@NotNull Project project) {
        myProject = project;
    }

    @NotNull
    protected abstract String getDisablingKey(@NotNull VirtualFile file);

    @NotNull
    @Override
    public final Function<FileEditor, RsEditorNotificationPanel> collectNotificationData(@NotNull Project project, @NotNull VirtualFile file) {
        return editor -> createNotificationPanel(file, editor, project);
    }

    @Nullable
    protected abstract RsEditorNotificationPanel createNotificationPanel(
        @NotNull VirtualFile file,
        @NotNull FileEditor editor,
        @NotNull Project project
    );

    protected void updateAllNotifications() {
        EditorNotifications.getInstance(myProject).updateAllNotifications();
    }

    protected void disableNotification(@NotNull VirtualFile file) {
        PropertiesComponent.getInstance(myProject).setValue(getDisablingKey(file), true);
    }

    protected boolean isNotificationDisabled(@NotNull VirtualFile file) {
        return PropertiesComponent.getInstance(myProject).getBoolean(getDisablingKey(file));
    }
}
