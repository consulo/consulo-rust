/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.ide.notifications;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.project.Project;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import java.util.List;

/**
 * Bridge class delegating to {@link NotificationUtils}.
 */
public final class TrustedProjectNotificationUtil {
    private TrustedProjectNotificationUtil() {
    }

    public static boolean confirmLoadingUntrustedProject(@NotNull Project project) {
        return NotificationUtils.confirmLoadingUntrustedProject(project);
    }
}
