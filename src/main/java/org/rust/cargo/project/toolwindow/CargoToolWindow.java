/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.cargo.project.toolwindow;

import com.intellij.ide.DefaultTreeExpander;
import com.intellij.ide.TreeExpander;
import com.intellij.openapi.actionSystem.*;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.wm.ToolWindowEP;
import com.intellij.openapi.wm.ToolWindowManager;
import com.intellij.openapi.wm.impl.ToolWindowManagerImpl;
import com.intellij.ui.ScrollPaneFactory;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.rust.cargo.project.model.CargoProject;
import org.rust.cargo.project.model.CargoProjectServiceUtil;
import org.rust.cargo.project.model.CargoProjectsService;
import org.rust.cargo.runconfig.RunConfigUtil;

import javax.swing.*;

public class CargoToolWindow {

    private static final Logger LOG = Logger.getInstance(CargoToolWindow.class);

    public static final DataKey<CargoProject> SELECTED_CARGO_PROJECT = DataKey.create("SELECTED_CARGO_PROJECT");

    public static final String CARGO_TOOLBAR_PLACE = "Cargo Toolbar";

    private static final String ID = "Cargo";

    private final Project project;
    @NotNull
    public final ActionToolbar toolbar;
    private final CargoProjectsTree projectTree;
    private final CargoProjectTreeStructure projectStructure;
    @NotNull
    public final TreeExpander treeExpander;
    @Nullable
    public CargoProject getSelectedProject() { return projectTree.getSelectedProject(); }
    @NotNull
    public final JComponent content;

    public CargoToolWindow(@NotNull Project project) {
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

    public static void initializeToolWindow(@NotNull Project project) {
        try {
            @SuppressWarnings("UnstableApiUsage")
            ToolWindowManager manager = ToolWindowManager.getInstance(project);
            if (!(manager instanceof ToolWindowManagerImpl managerImpl)) return;
            ToolWindowEP bean = ToolWindowEP.EP_NAME.getExtensionList().stream()
                .filter(ep -> ID.equals(ep.id))
                .findFirst().orElse(null);
            if (bean != null) {
                //noinspection deprecation,UnstableApiUsage
                managerImpl.initToolWindow(bean);
            }
        } catch (Exception e) {
            LOG.error("Unable to initialize " + ID + " tool window", e);
        }
    }

    public static boolean isRegistered(@NotNull Project project) {
        ToolWindowManager manager = ToolWindowManager.getInstance(project);
        return manager.getToolWindow(ID) != null;
    }

    // -- Factory --

    /** @deprecated Use {@link CargoToolWindowFactory} directly. */
    @Deprecated
    public static class Factory extends CargoToolWindowFactory {
    }
}
