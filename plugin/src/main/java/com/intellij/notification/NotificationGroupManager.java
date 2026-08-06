package com.intellij.notification;
import consulo.project.ui.notification.NotificationGroup;
/** IntelliJ-compat stub. */
public abstract class NotificationGroupManager {
    public static NotificationGroupManager getInstance() { return null; }
    public abstract NotificationGroup getNotificationGroup(String groupId);
}
