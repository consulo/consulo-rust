/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.notifications;

import consulo.project.ui.notification.NotificationGroup;
import jakarta.annotation.Nonnull;

/**
 * Notification groups used by the Rust plugin. The instances are also handed to the platform by
 * {@link RsNotificationGroupContributor} so that they appear in the notification settings.
 */
public final class RsNotifications {

    public static final String BUILD_LOG_GROUP_ID = "Rust Build Log";
    public static final String PLUGIN_GROUP_ID = "Rust Plugin";

    /** Log-only group used for background build progress messages. */
    public static final NotificationGroup BUILD_LOG = NotificationGroup.logOnlyGroup(BUILD_LOG_GROUP_ID);

    /** Balloon group used for user facing plugin notifications. */
    public static final NotificationGroup PLUGIN = NotificationGroup.balloonGroup(PLUGIN_GROUP_ID);

    public static final RsNotifications INSTANCE = new RsNotifications();

    private RsNotifications() {
    }

    @Nonnull
    public static NotificationGroup buildLogGroup() {
        return BUILD_LOG;
    }

    @Nonnull
    public static NotificationGroup pluginNotifications() {
        return PLUGIN;
    }
}
