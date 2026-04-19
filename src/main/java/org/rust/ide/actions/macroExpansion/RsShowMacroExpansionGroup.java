/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.ide.actions.macroExpansion;

import com.intellij.openapi.actionSystem.ActionPlaces;
import com.intellij.openapi.actionSystem.ActionUpdateThread;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.DefaultActionGroup;

/**
 * Action group for showing macros expansion actions in context menu.
 */
public class RsShowMacroExpansionGroup extends DefaultActionGroup {

    @Override
    public ActionUpdateThread getActionUpdateThread() {
        return ActionUpdateThread.BGT;
    }

    @Override
    public void update(AnActionEvent event) {
        boolean inEditorPopupMenu = ActionPlaces.EDITOR_POPUP.equals(event.getPlace());
        event.getPresentation().setEnabledAndVisible(
            inEditorPopupMenu && RsShowMacroExpansionActions.getMacroUnderCaret(event.getDataContext()) != null
        );
    }
}
