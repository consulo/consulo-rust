/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.cargo.project.toolwindow;

import com.intellij.openapi.actionSystem.PlatformDataKeys;
import com.intellij.openapi.project.DumbAware;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.SimpleToolWindowPanel;
import com.intellij.openapi.util.Key;
import com.intellij.openapi.wm.ToolWindow;
import com.intellij.openapi.wm.ToolWindowFactory;
import com.intellij.ui.content.Content;
import com.intellij.ui.content.ContentFactory;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.rust.cargo.project.model.CargoProjectServiceUtil;

public class CargoToolWindowFactory implements ToolWindowFactory, DumbAware {
    private final Object lock = new Object();
    private static final Key<Boolean> CARGO_TOOL_WINDOW_APPLICABLE = Key.create("CARGO_TOOL_WINDOW_APPLICABLE");

    @Override
    public void createToolWindowContent(@NotNull Project project, @NotNull ToolWindow toolWindow) {
        CargoProjectServiceUtil.guessAndSetupRustProject(project);
        CargoToolWindow cargoToolWindow = new CargoToolWindow(project);
        SimpleToolWindowPanel toolwindowPanel = new SimpleToolWindowPanel(true, false) {
            @Nullable
            @Override
            public Object getData(@NotNull String dataId) {
                if (CargoToolWindow.SELECTED_CARGO_PROJECT.is(dataId)) return cargoToolWindow.getSelectedProject();
                if (PlatformDataKeys.TREE_EXPANDER.is(dataId)) return cargoToolWindow.treeExpander;
                return super.getData(dataId);
            }
        };
        toolwindowPanel.setToolbar(cargoToolWindow.toolbar.getComponent());
        cargoToolWindow.toolbar.setTargetComponent(toolwindowPanel);
        toolwindowPanel.setContent(cargoToolWindow.content);

        Content tab = ContentFactory.getInstance().createContent(toolwindowPanel, "", false);
        toolWindow.getContentManager().addContent(tab);
    }

    @Override
    public boolean isApplicable(@NotNull Project project) {
        if (CargoToolWindow.isRegistered(project)) return false;
        var cargoProjects = CargoProjectServiceUtil.getCargoProjects(project);
        if (!cargoProjects.getHasAtLeastOneValidProject()) {
            boolean hasManifests = cargoProjects.suggestManifests().iterator().hasNext();
            if (!hasManifests) return false;
        }

        synchronized (lock) {
            Boolean res = project.getUserData(CARGO_TOOL_WINDOW_APPLICABLE);
            if (res == null) res = true;
            if (res) {
                project.putUserData(CARGO_TOOL_WINDOW_APPLICABLE, false);
            }
            return res;
        }
    }
}
