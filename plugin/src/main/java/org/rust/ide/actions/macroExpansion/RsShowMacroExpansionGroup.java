/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.ide.actions.macroExpansion;

import consulo.ui.ex.action.ActionPlaces;
import consulo.ui.ex.action.AnActionEvent;
import consulo.ui.ex.action.DefaultActionGroup;
import consulo.ui.ex.action.AnActionWithSyncUpdate;

/**
 * Action group for showing macros expansion actions in context menu.
 */
public class RsShowMacroExpansionGroup extends DefaultActionGroup implements AnActionWithSyncUpdate {


    @Override
    public void update(AnActionEvent event) {
        boolean inEditorPopupMenu = ActionPlaces.EDITOR_POPUP.equals(event.getPlace());
        event.getPresentation().setEnabledAndVisible(
            inEditorPopupMenu && RsShowMacroExpansionActions.getMacroUnderCaret(event.getDataContext()) != null
        );
    }
}
