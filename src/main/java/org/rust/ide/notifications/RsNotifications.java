/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.ide.notifications;

import com.intellij.notification.NotificationGroup;
import com.intellij.notification.NotificationGroupManager;
import org.jetbrains.annotations.NotNull;

public final class RsNotifications {

    public static final RsNotifications INSTANCE = new RsNotifications();

    private RsNotifications() {
    }

    @NotNull
    public static NotificationGroup buildLogGroup() {
        return NotificationGroupManager.getInstance().getNotificationGroup("Rust Build Log");
    }

    @NotNull
    public static NotificationGroup pluginNotifications() {
        return NotificationGroupManager.getInstance().getNotificationGroup("Rust Plugin");
    }
}
