/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.cargo.project.toolwindow;

import consulo.annotation.component.ActionImpl;
import consulo.annotation.component.ActionRef;
import consulo.ui.ex.action.AnSeparator;
import consulo.ui.ex.action.DefaultActionGroup;
import consulo.ui.ex.action.IdeActions;
import org.rust.cargo.project.model.AttachCargoProjectAction;
import org.rust.cargo.project.model.DetachCargoProjectAction;
import org.rust.cargo.runconfig.command.RunCargoCommandAction;
import org.rust.ide.actions.CargoEditSettingsAction;
import org.rust.ide.actions.RefreshCargoProjectsAction;
import org.rust.ide.actions.RsRunExternalLinterAction;
import org.rust.ide.actions.RustfmtCargoProjectAction;

/**
 * Toolbar of the Cargo tool window.
 * <p>
 * {@code ExpandAll} / {@code CollapseAll} operate on the {@link consulo.ui.ex.TreeExpander}
 * published by {@link CargoToolWindowFactory} under
 * {@link consulo.language.editor.PlatformDataKeys#TREE_EXPANDER}.
 */
@ActionImpl(id = CargoToolWindowActionGroup.ID, children = {
    @ActionRef(type = RefreshCargoProjectsAction.class),
    @ActionRef(type = AttachCargoProjectAction.class),
    @ActionRef(type = DetachCargoProjectAction.class),
    @ActionRef(type = AnSeparator.class),
    @ActionRef(type = RunCargoCommandAction.class),
    @ActionRef(type = RsRunExternalLinterAction.class),
    @ActionRef(type = RustfmtCargoProjectAction.class),
    @ActionRef(type = AnSeparator.class),
    @ActionRef(id = IdeActions.ACTION_EXPAND_ALL),
    @ActionRef(id = IdeActions.ACTION_COLLAPSE_ALL),
    @ActionRef(type = AnSeparator.class),
    @ActionRef(type = CargoEditSettingsAction.class)
})
public class CargoToolWindowActionGroup extends DefaultActionGroup {
    public static final String ID = "Rust.Cargo";
}
