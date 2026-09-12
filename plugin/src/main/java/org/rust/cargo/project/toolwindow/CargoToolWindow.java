/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.cargo.project.toolwindow;

import consulo.application.ApplicationManager;
import consulo.logging.Logger;
import consulo.project.Project;
import consulo.ui.ex.TreeExpander;
import consulo.ui.ex.action.ActionGroup;
import consulo.ui.ex.action.ActionManager;
import consulo.ui.ex.action.ActionToolbar;
import consulo.ui.ex.action.AnAction;
import consulo.ui.ex.action.DefaultActionGroup;
import consulo.ui.ex.awt.ScrollPaneFactory;
import consulo.ui.ex.awt.tree.DefaultTreeExpander;
import consulo.util.dataholder.Key;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import org.rust.cargo.project.model.CargoProject;
import org.rust.cargo.project.model.CargoProjectServiceUtil;
import org.rust.cargo.project.model.CargoProjectsService;
import org.rust.cargo.runconfig.RunConfigUtil;

import javax.swing.*;
import java.util.ArrayList;

/**
 * Content of the Cargo tool window: a toolbar plus a tree of Cargo projects,
 * their workspace members and their targets.
 */
public class CargoToolWindow {

    private static final Logger LOG = Logger.getInstance(CargoToolWindow.class);

    public static final Key<CargoProject> SELECTED_CARGO_PROJECT = Key.create("SELECTED_CARGO_PROJECT");

    public static final String CARGO_TOOLBAR_PLACE = "Cargo Toolbar";

    @Nonnull
    public final ActionToolbar toolbar;
    private final CargoProjectsTree projectTree;
    private final CargoProjectTreeStructure projectStructure;
    @Nonnull
    public final TreeExpander treeExpander;
    @Nonnull
    public final JComponent content;

    public CargoToolWindow(@Nonnull Project project) {
        ActionManager actionManager = ActionManager.getInstance();
        this.toolbar = actionManager.createActionToolbar(CARGO_TOOLBAR_PLACE, toolbarActions(actionManager), true);

        this.projectTree = new CargoProjectsTree();
        this.projectStructure = new CargoProjectTreeStructure(projectTree, project);

        this.treeExpander = new DefaultTreeExpander(projectTree) {
            @Override
            public boolean isCollapseAllVisible() {
                return RunConfigUtil.hasCargoProject(project);
            }

            @Override
            public boolean isExpandAllVisible() {
                return RunConfigUtil.hasCargoProject(project);
            }
        };

        this.content = ScrollPaneFactory.createScrollPane(projectTree, 0);

        project.getMessageBus().connect().subscribe(
            CargoProjectsService.CARGO_PROJECTS_TOPIC,
            (service, projects) -> ApplicationManager.getApplication().invokeLater(
                () -> projectStructure.updateCargoProjects(new ArrayList<>(projects))
            )
        );

        ApplicationManager.getApplication().invokeLater(() -> {
            if (project.isDisposed()) return;
            projectStructure.updateCargoProjects(
                new ArrayList<>(CargoProjectServiceUtil.getCargoProjects(project).getAllProjects())
            );
        });
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

    @Nullable
    public CargoProject getSelectedProject() {
        return projectTree.getSelectedProject();
    }
}
