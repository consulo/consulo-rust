/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.ide.actions;

import org.rust.ide.console.RunRustConsoleAction;
import org.rust.ide.actions.macroExpansion.RsShowMacroExpansionGroup;
import org.rust.icons.RsIcons;
import consulo.rust.localize.RustLocalize;
import consulo.ui.ex.action.IdeActions;
import consulo.annotation.component.ActionRefAnchor;
import consulo.annotation.component.ActionRef;
import consulo.annotation.component.ActionParentRef;
import consulo.annotation.component.ActionImpl;
import consulo.ui.ex.action.AnActionEvent;
import consulo.ui.ex.action.DefaultActionGroup;
import consulo.application.dumb.DumbAware;
import consulo.project.Project;
import consulo.ui.ex.action.AnActionWithSyncUpdate;
import org.rust.cargo.runconfig.RunConfigUtil;

/**
 * The "Rust" submenu of the Tools menu and of the editor, editor tab and project view context menus.
 */
@ActionImpl(
    id = "Rust.Tools",
    children = {
        @ActionRef(type = RsShowMacroExpansionGroup.class),
        @ActionRef(type = RustfmtFileAction.class),
        @ActionRef(type = RustfmtCargoProjectAction.class),
        @ActionRef(type = RunRustConsoleAction.class),
        @ActionRef(type = ShareInPlaygroundAction.class)
    },
    parents = {
        @ActionParentRef(value = @ActionRef(id = IdeActions.TOOLS_MENU), anchor = ActionRefAnchor.LAST),
        @ActionParentRef(value = @ActionRef(id = IdeActions.GROUP_PROJECT_VIEW_POPUP), anchor = ActionRefAnchor.LAST),
        @ActionParentRef(value = @ActionRef(id = IdeActions.GROUP_EDITOR_POPUP), anchor = ActionRefAnchor.LAST),
        @ActionParentRef(value = @ActionRef(id = IdeActions.GROUP_EDITOR_TAB_POPUP), anchor = ActionRefAnchor.LAST)
    }
)
public class RsToolsActionGroup extends DefaultActionGroup implements DumbAware, AnActionWithSyncUpdate {
    public RsToolsActionGroup() {
        getTemplatePresentation().setText(RustLocalize.groupRustToolsText());
        getTemplatePresentation().setIcon(RsIcons.RUST);
        setPopup(true);
    }



    @Override
    public void update(AnActionEvent e) {
        Project project = e.getData(consulo.project.Project.KEY);
        if (project == null) return;
        e.getPresentation().setEnabledAndVisible(RunConfigUtil.hasCargoProject(project));
    }
}
