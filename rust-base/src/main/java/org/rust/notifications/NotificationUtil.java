/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.notifications;

import consulo.disposer.Disposable;
import consulo.project.Project;
import consulo.project.ui.notification.NotificationType;
import consulo.project.ui.notification.event.NotificationListener;
import consulo.ui.ex.action.AnAction;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

import javax.swing.event.HyperlinkListener;
import java.awt.Component;

/**
 * Thin alias over {@link NotificationUtils}; every method forwards so that both spellings behave
 * identically.
 */
public final class NotificationUtil {

    private NotificationUtil() {
    }

    public static void showBalloon(
        @Nonnull Project project,
        @Nonnull String content,
        @Nonnull NotificationType type,
        @Nullable AnAction action
    ) {
        NotificationUtils.showBalloon(project, "", content, type, action, null);
    }

    public static void showBalloon(
        @Nonnull Project project,
        @Nonnull String title,
        @Nonnull String content,
        @Nonnull NotificationType type,
        @Nullable AnAction action,
        @Nullable NotificationListener listener
    ) {
        NotificationUtils.showBalloon(project, title, content, type, action, listener);
    }

    public static void showBalloon(
        @Nonnull Component component,
        @Nonnull String content,
        @Nonnull NotificationType type,
        @Nullable Disposable disposable,
        @Nullable HyperlinkListener listener
    ) {
        NotificationUtils.showComponentBalloon(component, content, type, disposable, listener);
    }

    public static void showBalloonWithoutProject(@Nonnull String content, @Nonnull NotificationType type) {
        NotificationUtils.showBalloonWithoutProject(content, type);
    }

    public static void setStatusBarText(@Nonnull Project project, @Nonnull String text) {
        NotificationUtils.setStatusBarText(project, text);
    }
}
