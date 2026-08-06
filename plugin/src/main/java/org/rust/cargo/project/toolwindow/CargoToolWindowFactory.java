/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.cargo.project.toolwindow;

import consulo.application.dumb.DumbAware;
import consulo.language.editor.PlatformDataKeys;
import consulo.localize.LocalizeValue;
import consulo.project.Project;
import consulo.project.ui.wm.ToolWindowFactory;
import consulo.ui.ex.awt.SimpleToolWindowPanel;
import consulo.ui.ex.content.Content;
import consulo.ui.ex.content.ContentFactory;
import consulo.ui.ex.toolWindow.ToolWindow;
import consulo.ui.ex.toolWindow.ToolWindowAnchor;
import consulo.ui.image.Image;
import consulo.util.dataholder.Key;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import org.rust.cargo.project.model.CargoProjectServiceUtil;

public class CargoToolWindowFactory implements ToolWindowFactory, DumbAware {
    public static final String ID = "Cargo";
    private final Object lock = new Object();
    private static final Key<Boolean> CARGO_TOOL_WINDOW_APPLICABLE = Key.create("CARGO_TOOL_WINDOW_APPLICABLE");

    @Nonnull
    @Override
    public String getId() { return ID; }

    @Nonnull
    @Override
    public ToolWindowAnchor getAnchor() { return ToolWindowAnchor.RIGHT; }

    @Override
    public Image getIcon() { return org.rust.cargo.icons.CargoIcons.ICON; }

    @Nonnull
    @Override
    public LocalizeValue getDisplayName() { return LocalizeValue.of("Cargo"); }

    @Override
    public void createToolWindowContent(@Nonnull Project project, @Nonnull ToolWindow toolWindow) {
        CargoProjectServiceUtil.guessAndSetupRustProject(project);
        CargoToolWindow cargoToolWindow = new CargoToolWindow(project);
        SimpleToolWindowPanel toolwindowPanel = new SimpleToolWindowPanel(true, false) {
            @Nullable
            @Override
            public Object getData(@Nonnull Key<?> dataId) {
                if (CargoToolWindow.SELECTED_CARGO_PROJECT == dataId) return cargoToolWindow.getSelectedProject();
                if (PlatformDataKeys.TREE_EXPANDER == dataId) return cargoToolWindow.treeExpander;
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
    public boolean shouldBeAvailable(@Nonnull Project project) {
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
