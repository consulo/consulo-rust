/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.ide.notifications;

import consulo.annotation.component.ExtensionImpl;
import consulo.project.ui.notification.NotificationGroup;
import consulo.project.ui.notification.NotificationGroupContributor;
import jakarta.annotation.Nonnull;

import java.util.function.Consumer;

/**
 * Publishes the plugin notification groups so the platform can resolve them by id and show them
 * in the notification settings.
 */
@ExtensionImpl
public class RsNotificationGroupContributor implements NotificationGroupContributor {

    @Override
    public void contribute(@Nonnull Consumer<NotificationGroup> consumer) {
        consumer.accept(RsNotifications.PLUGIN);
        consumer.accept(RsNotifications.BUILD_LOG);
    }
}
