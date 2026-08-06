/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.ide.notifications;

import consulo.webBrowser.BrowserUtil;
import consulo.component.PropertiesComponent;
import com.intellij.notification.NotificationGroupManager;
import consulo.project.ui.notification.NotificationType;
import consulo.project.ui.notification.Notification;
import consulo.project.ui.notification.Notifications;
import consulo.ui.ex.action.AnAction;
import consulo.ui.ex.action.AnActionEvent;
import consulo.application.ApplicationManager;
import consulo.project.Project;
import consulo.project.startup.PostStartupActivity;
import com.intellij.util.PlatformUtils;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import org.rust.RsBundle;
import org.rust.cargo.runconfig.HasCargoProjectUtil;

public class RsIdePromotionStartupActivity implements PostStartupActivity {

    private static final String DO_NOT_SHOW_KEY = "com.jetbrains.rust.ide.promotion";

    @Override
    public void runActivity(@Nonnull Project project, @Nonnull consulo.ui.UIAccess uiAccess) {
        if (consulo.application.Application.get().getInstance(consulo.component.PropertiesComponent.class).getBoolean(DO_NOT_SHOW_KEY, false)) return;
        if (ApplicationManager.getApplication().isUnitTestMode() || !HasCargoProjectUtil.getHasCargoProject(project)) return;
        // IdeaUltimate / baselineVersion is IntelliJ-only; always skip this promotion in Consulo
        if (true) return;

        Notification notification = NotificationGroupManager.getInstance()
            .getNotificationGroup("Rust Plugin Promotion")
            .createNotification(
                RsBundle.message("notification.title.introducing.rustrover.dedicated.rust.ide.by.jetbrains"),
                RsBundle.message("notification.content.rust.plugin.no.longer.officialy.supporter"),
                NotificationType.INFORMATION,
                null
            );

        notification.addAction(new AnAction(RsBundle.message("action.download.rustrover.text")) {
            @Override
            public void actionPerformed(@Nonnull AnActionEvent e) {
                notification.expire();
                BrowserUtil.browse("https://www.jetbrains.com/rustrover/download");
            }
        });

        notification.addAction(new AnAction(RsBundle.message("don.t.show.again")) {
            @Override
            public void actionPerformed(@Nonnull AnActionEvent e) {
                consulo.application.Application.get().getInstance(consulo.component.PropertiesComponent.class).setValue(DO_NOT_SHOW_KEY, true);
                notification.expire();
            }
        });

        Notifications.Bus.notify(notification, project);
    }
}
