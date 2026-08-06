/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.cargo.project.toolwindow;

import consulo.ui.ex.awt.tree.DefaultTreeExpander;
import consulo.ui.ex.TreeExpander;
import consulo.ui.ex.action.AnAction;
import consulo.ui.ex.action.AnActionEvent;
import consulo.ui.ex.action.ActionManager;
import consulo.ui.ex.action.ActionToolbar;
import consulo.ui.ex.action.ActionGroup;
import consulo.ui.ex.action.DefaultActionGroup;
import consulo.ui.ex.action.Presentation;
import consulo.ui.ex.action.DumbAwareAction;
import consulo.util.dataholder.Key;
import consulo.dataContext.DataContext;
import consulo.dataContext.DataProvider;
import consulo.language.editor.CommonDataKeys;
import consulo.language.editor.PlatformDataKeys;
import consulo.application.ApplicationManager;
import consulo.logging.Logger;
import consulo.project.Project;
import com.intellij.openapi.wm.ToolWindowEP;
import consulo.project.ui.wm.ToolWindowManager;
import com.intellij.openapi.wm.impl.ToolWindowManagerImpl;
import consulo.ui.ex.awt.ScrollPaneFactory;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import org.rust.cargo.project.model.CargoProject;
import org.rust.cargo.project.model.CargoProjectServiceUtil;
import org.rust.cargo.project.model.CargoProjectsService;
import org.rust.cargo.runconfig.RunConfigUtil;

import javax.swing.*;

public class CargoToolWindow {

    private static final Logger LOG = Logger.getInstance(CargoToolWindow.class);

    public static final Key<CargoProject> SELECTED_CARGO_PROJECT = Key.create("SELECTED_CARGO_PROJECT");

    public static final String CARGO_TOOLBAR_PLACE = "Cargo Toolbar";

    private static final String ID = "Cargo";

    private final Project project;
    @Nonnull
    public final ActionToolbar toolbar;
    private final CargoProjectsTree projectTree;
    private final CargoProjectTreeStructure projectStructure;
    @Nonnull
    public final TreeExpander treeExpander;
    @Nullable
    public CargoProject getSelectedProject() { return projectTree.getSelectedProject(); }
    @Nonnull
    public final JComponent content;

    public CargoToolWindow(@Nonnull Project project) {
        this.project = project;
        ActionManager actionManager = ActionManager.getInstance();
        this.toolbar = actionManager.createActionToolbar(
            CARGO_TOOLBAR_PLACE,
            (DefaultActionGroup) actionManager.getAction("Rust.Cargo"),
            true
        );

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
            (service, projects) -> ApplicationManager.getApplication().invokeLater(() ->
                projectStructure.updateCargoProjects(new java.util.ArrayList<>(projects))
            )
        );

        ApplicationManager.getApplication().invokeLater(() ->
            projectStructure.updateCargoProjects(
                new java.util.ArrayList<>(CargoProjectServiceUtil.getCargoProjects(project).getAllProjects())
            )
        );
    }

    public static void initializeToolWindow(@Nonnull Project project) {
        // Consulo registers tool windows via @ExtensionImpl ToolWindowFactory, not runtime ToolWindowEP
    }

    public static boolean isRegistered(@Nonnull Project project) {
        ToolWindowManager manager = ToolWindowManager.getInstance(project);
        return manager.getToolWindow(ID) != null;
    }

    // -- Factory --

    /** @deprecated Use {@link CargoToolWindowFactory} directly. */
    @Deprecated
    public static class Factory extends CargoToolWindowFactory {
    }
}
