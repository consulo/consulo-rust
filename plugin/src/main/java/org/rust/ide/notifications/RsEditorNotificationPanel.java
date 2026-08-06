/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.ide.notifications;

import consulo.ide.impl.idea.ui.EditorNotificationPanel;
import jakarta.annotation.Nonnull;

public class RsEditorNotificationPanel extends EditorNotificationPanel {

    public static final String NOTIFICATION_PANEL_PLACE = "RsEditorNotificationPanel";

    @SuppressWarnings("unused")
    private final String myDebugId;

    public RsEditorNotificationPanel(@Nonnull String debugId) {
        myDebugId = debugId;
    }

    @Nonnull
    public String getActionPlace() {
        return NOTIFICATION_PANEL_PLACE;
    }
}
