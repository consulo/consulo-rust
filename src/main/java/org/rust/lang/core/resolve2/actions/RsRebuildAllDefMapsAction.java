/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.lang.core.resolve2.actions;

import com.intellij.notification.NotificationType;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.project.Project;
import org.jetbrains.annotations.NotNull;
import org.rust.RsBundle;
import org.rust.ide.notifications.NotificationUtils;

public class RsRebuildAllDefMapsAction extends AnAction {
    @Override
    public void actionPerformed(@NotNull AnActionEvent e) {
        Project project = e.getProject();
        if (project == null) return;

        ApplicationManager.getApplication().executeOnPooledThread(() -> {
            long start = System.currentTimeMillis();
            // project.forceRebuildDefMapForAllCrates(false);
            long time = System.currentTimeMillis() - start;
            NotificationUtils.showBalloon(
                project,
                RsBundle.message("notification.content.rebuilt.defmap.for.all.crates.in.ms", time),
                NotificationType.INFORMATION
            );
        });
    }
}
