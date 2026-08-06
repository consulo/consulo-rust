/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.ide.notifications;
import consulo.ide.IdeBundle;

import com.intellij.ide.impl.TrustedProjects;
import consulo.project.ui.notification.event.NotificationListener;
import consulo.project.ui.notification.NotificationType;
import consulo.project.ui.notification.Notifications;
import consulo.disposer.Disposable;
import consulo.ui.ex.action.AnAction;
import consulo.application.ApplicationManager;
import consulo.project.Project;
import consulo.ui.ex.popup.Balloon;
import consulo.ui.ex.popup.JBPopupFactory;

import consulo.project.ui.wm.WindowManager;
import consulo.ui.ex.RelativePoint;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import org.rust.RsBundle;

import javax.swing.event.HyperlinkListener;
import java.awt.*;

public final class NotificationUtil {

    private NotificationUtil() {
    }

    public static void showBalloon(
        @Nonnull Project project,
        @Nonnull  String content,
        @Nonnull NotificationType type,
        @Nullable AnAction action
    ) {
        showBalloon(project, "", content, type, action, null);
    }

    public static void showBalloon(
        @Nonnull Project project,
        @Nonnull  String title,
        @Nonnull  String content,
        @Nonnull NotificationType type,
        @Nullable AnAction action,
        @Nullable NotificationListener listener
    ) {
        var notification = RsNotifications.pluginNotifications().createNotification(title, content, type, listener);
        if (action != null) {
            notification.addAction(action);
        }
        Notifications.Bus.notify(notification, project);
    }

    public static void showBalloon(
        @Nonnull Component component,
        @Nonnull  String content,
        @Nonnull consulo.ui.NotificationType type,
        @Nullable Disposable disposable,
        @Nullable HyperlinkListener listener
    ) {
        if (disposable == null) {
            disposable = ApplicationManager.getApplication();
        }
        JBPopupFactory popupFactory = JBPopupFactory.getInstance();
        if (popupFactory == null) return;
        Balloon balloon = popupFactory.createHtmlTextBalloonBuilder(content, type, listener)
            .setShadow(false)
            .setAnimationCycle(200)
            .setHideOnLinkClick(true)
            .setDisposable(disposable)
            .createBalloon();
        balloon.setAnimationEnabled(false);
        int x;
        int y;
        Balloon.Position position;
        Dimension size = component.getSize();
        if (size == null) {
            x = 0;
            y = 0;
            position = Balloon.Position.above;
        } else {
            x = size.width / 2;
            y = 0;
            position = Balloon.Position.above;
        }
        balloon.show(new RelativePoint(component, new Point(x, y)), position);
    }

    public static void showBalloonWithoutProject(
        @Nonnull  String content,
        @Nonnull NotificationType type
    ) {
        var notification = RsNotifications.pluginNotifications().createNotification(content, type);
        Notifications.Bus.notify(notification);
    }

    public static void setStatusBarText(
        @Nonnull Project project,
        @Nonnull  String text
    ) {
        var statusBar = WindowManager.getInstance().getStatusBar(project);
        if (statusBar != null) {
            // statusBar.setInfo not in Consulo API
        }
    }

    @SuppressWarnings("UnstableApiUsage")
    public static boolean confirmLoadingUntrustedProject(@Nonnull Project project) {
        return TrustedProjects.isTrusted(project) || com.intellij.ide.impl.TrustedProjects.confirmLoadingUntrustedProject(
            project,
            IdeBundle.message("untrusted.project.dialog.title", RsBundle.message("cargo"), 1),
            IdeBundle.message("untrusted.project.dialog.text", RsBundle.message("cargo"), 1),
            IdeBundle.message("untrusted.project.dialog.trust.button"),
            IdeBundle.message("untrusted.project.dialog.distrust.button")
        );
    }
}
