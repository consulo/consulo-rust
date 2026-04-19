/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.ide.notifications;

import com.intellij.ui.EditorNotificationPanel;
import org.jetbrains.annotations.NotNull;

public class RsEditorNotificationPanel extends EditorNotificationPanel {

    public static final String NOTIFICATION_PANEL_PLACE = "RsEditorNotificationPanel";

    @SuppressWarnings("unused")
    private final String myDebugId;

    public RsEditorNotificationPanel(@NotNull String debugId) {
        myDebugId = debugId;
    }

    @NotNull
    @Override
    public String getActionPlace() {
        return NOTIFICATION_PANEL_PLACE;
    }
}
