/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.ide.notifications;

import consulo.project.ui.notification.NotificationGroup;
import com.intellij.notification.NotificationGroupManager;
import jakarta.annotation.Nonnull;

public final class RsNotifications {

    public static final RsNotifications INSTANCE = new RsNotifications();

    private RsNotifications() {
    }

    @Nonnull
    public static NotificationGroup buildLogGroup() {
        return NotificationGroupManager.getInstance().getNotificationGroup("Rust Build Log");
    }

    @Nonnull
    public static NotificationGroup pluginNotifications() {
        return NotificationGroupManager.getInstance().getNotificationGroup("Rust Plugin");
    }
}
