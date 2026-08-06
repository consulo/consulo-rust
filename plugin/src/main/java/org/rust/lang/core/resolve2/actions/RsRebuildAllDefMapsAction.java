/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.lang.core.resolve2.actions;

import consulo.project.ui.notification.NotificationType;
import consulo.ui.ex.action.AnAction;
import consulo.ui.ex.action.AnActionEvent;
import consulo.application.ApplicationManager;
import consulo.project.Project;
import jakarta.annotation.Nonnull;
import org.rust.RsBundle;
import org.rust.ide.notifications.NotificationUtils;

public class RsRebuildAllDefMapsAction extends AnAction {
    @Override
    public void actionPerformed(@Nonnull AnActionEvent e) {
        Project project = e.getData(consulo.project.Project.KEY);
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
