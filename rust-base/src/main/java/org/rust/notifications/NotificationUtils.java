/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.notifications;

import consulo.project.ui.notification.Notification;
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
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Point;
import consulo.project.ui.wm.StatusBar;

public final class NotificationUtils {

    private NotificationUtils() {
    }

    public static void showBalloon(@Nonnull Project project, @Nonnull String content, @Nonnull NotificationType type) {
        showBalloon(project, "", content, type, null, null);
    }

    public static void showBalloon(@Nonnull Project project, @Nonnull String content, @Nonnull NotificationType type, @Nullable AnAction action) {
        showBalloon(project, "", content, type, action, null);
    }

    public static void showBalloon(@Nonnull Project project, @Nonnull String title, @Nonnull String content, @Nonnull NotificationType type) {
        showBalloon(project, title, content, type, null, null);
    }

    public static void showBalloon(
        @Nonnull Project project,
        @Nonnull String title,
        @Nonnull String content,
        @Nonnull NotificationType type,
        @Nullable AnAction action,
        @Nullable NotificationListener listener
    ) {
        Notification notification = RsNotifications.pluginNotifications().createNotification(title, content, type, listener);
        if (action != null) {
            notification.addAction(action);
        }
        Notifications.Bus.notify(notification, project);
    }

    public static void showComponentBalloon(
        @Nonnull Component component,
        @Nonnull String content,
        @Nonnull NotificationType type,
        @Nullable Disposable disposable,
        @Nullable HyperlinkListener listener
    ) {
        if (disposable == null) {
            disposable = ApplicationManager.getApplication();
        }
        JBPopupFactory popupFactory = JBPopupFactory.getInstance();
        if (popupFactory == null) return;
        Balloon balloon = popupFactory.createHtmlTextBalloonBuilder(content, type.toUI(), listener)
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

    public static void showBalloonWithoutProject(@Nonnull String content, @Nonnull NotificationType type) {
        Notification notification = RsNotifications.pluginNotifications().createNotification(content, type);
        Notifications.Bus.notify(notification);
    }

    /**
     * Shows {@code text} as a transient balloon anchored to the project status bar. Safe to call from
     * any thread; the balloon is shown on the UI thread.
     */
    public static void setStatusBarText(@Nonnull Project project, @Nonnull String text) {
        ApplicationManager.getApplication().invokeLater(() -> {
            if (project.isDisposed()) return;
            StatusBar statusBar = WindowManager.getInstance().getStatusBar(project);
            if (statusBar != null) {
                statusBar.notifyProgressByBalloon(consulo.ui.NotificationType.INFO, text);
            }
        });
    }
}
