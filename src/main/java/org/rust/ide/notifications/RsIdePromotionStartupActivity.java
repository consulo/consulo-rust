/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.ide.notifications;

import com.intellij.ide.BrowserUtil;
import com.intellij.ide.util.PropertiesComponent;
import com.intellij.notification.NotificationGroupManager;
import com.intellij.notification.NotificationType;
import com.intellij.notification.Notification;
import com.intellij.notification.Notifications;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.application.ApplicationInfo;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.startup.ProjectActivity;
import com.intellij.util.PlatformUtils;
import kotlin.coroutines.Continuation;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.rust.RsBundle;
import org.rust.cargo.runconfig.HasCargoProjectUtil;

public class RsIdePromotionStartupActivity implements ProjectActivity {

    private static final String DO_NOT_SHOW_KEY = "com.jetbrains.rust.ide.promotion";

    @Nullable
    @Override
    public Object execute(@NotNull Project project, @NotNull Continuation<? super kotlin.Unit> continuation) {
        if (PropertiesComponent.getInstance().getBoolean(DO_NOT_SHOW_KEY, false)) return null;
        if (ApplicationManager.getApplication().isUnitTestMode() || !HasCargoProjectUtil.getHasCargoProject(project)) return null;
        if (PlatformUtils.isIdeaUltimate() && ApplicationInfo.getInstance().getBuild().getBaselineVersion() >= 241) {
            return null;
        }

        Notification notification = NotificationGroupManager.getInstance()
            .getNotificationGroup("Rust Plugin Promotion")
            .createNotification(
                RsBundle.message("notification.title.introducing.rustrover.dedicated.rust.ide.by.jetbrains"),
                RsBundle.message("notification.content.rust.plugin.no.longer.officialy.supporter"),
                NotificationType.INFORMATION
            );

        notification.addAction(new AnAction(RsBundle.message("action.download.rustrover.text")) {
            @Override
            public void actionPerformed(@NotNull AnActionEvent e) {
                notification.expire();
                BrowserUtil.browse("https://www.jetbrains.com/rustrover/download");
            }
        });

        notification.addAction(new AnAction(RsBundle.message("don.t.show.again")) {
            @Override
            public void actionPerformed(@NotNull AnActionEvent e) {
                PropertiesComponent.getInstance().setValue(DO_NOT_SHOW_KEY, true);
                notification.expire();
            }
        });

        Notifications.Bus.notify(notification, project);
        return null;
    }
}
