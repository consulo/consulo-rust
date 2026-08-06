/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.ide.notifications;
import consulo.codeEditor.Editor;
import consulo.project.Project;
import consulo.language.psi.PsiElement;
import consulo.language.psi.PsiFile;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import java.util.List;

/**
 * Bridge class delegating to {@link NotificationUtils}.
 */
public final class TrustedProjectNotificationUtil {
    private TrustedProjectNotificationUtil() {
    }

    public static boolean confirmLoadingUntrustedProject(@Nonnull Project project) {
        return NotificationUtils.confirmLoadingUntrustedProject(project);
    }
}
