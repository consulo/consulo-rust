/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.cargo.project.toolwindow;

import consulo.dataContext.DataSink;
import consulo.dataContext.UiDataProvider;
import consulo.language.editor.PlatformDataKeys;
import consulo.logging.Logger;
import consulo.project.Project;
import consulo.ui.Component;
import consulo.ui.Tree;
import consulo.ui.TreeNode;
import consulo.ui.ex.TreeExpander;
import consulo.ui.ex.action.ActionGroup;
import consulo.ui.ex.action.ActionManager;
import consulo.ui.ex.action.ActionToolbar;
import consulo.ui.ex.action.AnAction;
import consulo.ui.ex.action.DefaultActionGroup;
import consulo.ui.layout.DockLayout;
import consulo.ui.layout.ScrollableLayout;
import consulo.util.dataholder.Key;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import org.rust.cargo.project.model.CargoProject;
import org.rust.cargo.project.model.CargoProjectServiceUtil;
import org.rust.cargo.project.model.CargoProjectsListener;
import org.rust.cargo.project.model.CargoProjectsService;
import org.rust.cargo.runconfig.RunConfigUtil;

import java.util.ArrayList;
import java.util.Collection;

/**
 * Content of the Cargo tool window: a toolbar above a tree of Cargo projects, their workspace members
 * and their targets.
 */
public class CargoToolWindow {

    private static final Logger LOG = Logger.getInstance(CargoToolWindow.class);

    public static final Key<CargoProject> SELECTED_CARGO_PROJECT = Key.create("SELECTED_CARGO_PROJECT");

    public static final String CARGO_TOOLBAR_PLACE = "Cargo Toolbar";

    @Nonnull
    public final ActionToolbar toolbar;
    @Nonnull
    public final TreeExpander treeExpander;

    private final Tree<CargoTreeNode> myTree;
    private final CargoTreeModel myModel;
    private final DockLayout myRoot;

    public CargoToolWindow(@Nonnull Project project) {
        ActionManager actionManager = ActionManager.getInstance();
        toolbar = actionManager.createActionToolbar(CARGO_TOOLBAR_PLACE, toolbarActions(actionManager), true);

        myModel = new CargoTreeModel();
        myTree = Tree.create(myModel);

        treeExpander = new TreeExpander() {
            @Override
            public void expandAll() {
                myTree.expandAll();
            }

            @Override
            public boolean canExpand() {
                return myTree.isExpandCollapseAllSupported() && RunConfigUtil.hasCargoProject(project);
            }

            @Override
            public void collapseAll() {
                myTree.collapseAll();
            }

            @Override
            public boolean canCollapse() {
                return myTree.isExpandCollapseAllSupported() && RunConfigUtil.hasCargoProject(project);
            }

            @Override
            public boolean isExpandAllVisible() {
                return RunConfigUtil.hasCargoProject(project);
            }

            @Override
            public boolean isCollapseAllVisible() {
                return RunConfigUtil.hasCargoProject(project);
            }
        };

        myRoot = DockLayout.create();
        myRoot.top(toolbar.getUIComponent());
        myRoot.center(ScrollableLayout.create(myTree));
        myRoot.putUserData(UiDataProvider.KEY, (UiDataProvider) this::uiDataSnapshot);

        toolbar.setTargetUIComponent(myRoot);

        project.getMessageBus().connect().subscribe(
            CargoProjectsService.CARGO_PROJECTS_TOPIC,
            (CargoProjectsListener) (service, projects) -> setCargoProjects(projects)
        );

        setCargoProjects(CargoProjectServiceUtil.getCargoProjects(project).getAllProjects());
    }

    private void uiDataSnapshot(@Nonnull DataSink sink) {
        sink.set(SELECTED_CARGO_PROJECT, getSelectedProject());
        sink.set(PlatformDataKeys.TREE_EXPANDER, treeExpander);
    }

    private void setCargoProjects(@Nonnull Collection<CargoProject> cargoProjects) {
        myModel.setCargoProjects(new ArrayList<>(cargoProjects));
        myTree.refreshAll();
    }

    @Nonnull
    public Component getComponent() {
        return myRoot;
    }

    @Nullable
    public CargoProject getSelectedProject() {
        TreeNode<CargoTreeNode> node = myTree.getSelectedNode();
        CargoTreeNode value = node == null ? null : node.getValue();
        return value == null ? null : value.cargoProject();
    }

    @Nonnull
    private static ActionGroup toolbarActions(@Nonnull ActionManager actionManager) {
        AnAction action = actionManager.getAction(CargoToolWindowActionGroup.ID);
        if (action instanceof ActionGroup group) {
            return group;
        }
        LOG.warn("Action group " + CargoToolWindowActionGroup.ID + " is not registered, Cargo toolbar will be empty");
        return new DefaultActionGroup();
    }
}
