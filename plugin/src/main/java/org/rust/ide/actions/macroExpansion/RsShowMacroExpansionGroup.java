/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.ide.actions.macroExpansion;

import consulo.util.concurrent.coroutine.Coroutine;
import consulo.ui.ex.action.coroutine.ActionSafeReadLock;
import consulo.ui.ex.action.AnActionWithAsyncUpdate;
import consulo.rust.localize.RustLocalize;
import consulo.ui.ex.action.AnSeparator;
import consulo.annotation.component.ActionRef;
import consulo.annotation.component.ActionImpl;
import consulo.ui.ex.action.ActionPlaces;
import consulo.ui.ex.action.AnActionEvent;
import consulo.ui.ex.action.DefaultActionGroup;

/**
 * Action group for showing macros expansion actions in context menu.
 */
@ActionImpl(
    id = "Rust.MacroExpansionActions",
    children = {
        @ActionRef(type = RsShowRecursiveMacroExpansionAction.class),
        @ActionRef(type = RsShowSingleStepMacroExpansionAction.class),
        @ActionRef(type = AnSeparator.class)
    }
)
public class RsShowMacroExpansionGroup extends DefaultActionGroup implements AnActionWithAsyncUpdate {
    public RsShowMacroExpansionGroup() {
        getTemplatePresentation().setText(RustLocalize.groupRustMacroexpansionactionsText());
    }



    @Override
    public Coroutine<?, ?> updateAsync(AnActionEvent event) {
        boolean inEditorPopupMenu = ActionPlaces.EDITOR_POPUP.equals(event.getPlace());
        return ActionSafeReadLock.run(event, presentation -> presentation.setEnabledAndVisible(
            inEditorPopupMenu && RsShowMacroExpansionActions.getMacroUnderCaret(event.getDataContext()) != null
        )).toCoroutine();
    }
}
