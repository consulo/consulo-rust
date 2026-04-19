/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.ide.notifications;

import com.intellij.ide.IdeBundle;
import com.intellij.ide.impl.TrustedProjects;
import com.intellij.notification.NotificationListener;
import com.intellij.notification.NotificationType;
import com.intellij.notification.Notifications;
import com.intellij.openapi.Disposable;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.MessageType;
import com.intellij.openapi.ui.popup.Balloon;
import com.intellij.openapi.ui.popup.JBPopupFactory;
import com.intellij.openapi.util.NlsContexts;
import com.intellij.openapi.wm.WindowManager;
import com.intellij.ui.awt.RelativePoint;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.rust.RsBundle;

import javax.swing.event.HyperlinkListener;
import java.awt.*;

public final class NotificationUtil {

    private NotificationUtil() {
    }

    public static void showBalloon(
        @NotNull Project project,
        @NotNull @NlsContexts.NotificationContent String content,
        @NotNull NotificationType type,
        @Nullable AnAction action
    ) {
        showBalloon(project, "", content, type, action, null);
    }

    public static void showBalloon(
        @NotNull Project project,
        @NotNull @NlsContexts.NotificationTitle String title,
        @NotNull @NlsContexts.NotificationContent String content,
        @NotNull NotificationType type,
        @Nullable AnAction action,
        @Nullable NotificationListener listener
    ) {
        var notification = RsNotifications.pluginNotifications().createNotification(title, content, type);
        if (listener != null) {
            notification.setListener(listener);
        }
        if (action != null) {
            notification.addAction(action);
        }
        Notifications.Bus.notify(notification, project);
    }

    public static void showBalloon(
        @NotNull Component component,
        @NotNull @NlsContexts.NotificationContent String content,
        @NotNull MessageType type,
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
        @NotNull @NlsContexts.NotificationContent String content,
        @NotNull NotificationType type
    ) {
        var notification = RsNotifications.pluginNotifications().createNotification(content, type);
        Notifications.Bus.notify(notification);
    }

    public static void setStatusBarText(
        @NotNull Project project,
        @NotNull @NlsContexts.StatusBarText String text
    ) {
        var statusBar = WindowManager.getInstance().getStatusBar(project);
        if (statusBar != null) {
            statusBar.setInfo(text);
        }
    }

    @SuppressWarnings("UnstableApiUsage")
    public static boolean confirmLoadingUntrustedProject(@NotNull Project project) {
        return TrustedProjects.isTrusted(project) || com.intellij.ide.impl.TrustedProjects.confirmLoadingUntrustedProject(
            project,
            IdeBundle.message("untrusted.project.dialog.title", RsBundle.message("cargo"), 1),
            IdeBundle.message("untrusted.project.dialog.text", RsBundle.message("cargo"), 1),
            IdeBundle.message("untrusted.project.dialog.trust.button"),
            IdeBundle.message("untrusted.project.dialog.distrust.button")
        );
    }
}
