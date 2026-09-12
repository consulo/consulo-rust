/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.cargo.project.toolwindow;

import consulo.annotation.component.ExtensionImpl;
import consulo.application.dumb.DumbAware;
import consulo.dataContext.DataSink;
import consulo.language.editor.PlatformDataKeys;
import consulo.localize.LocalizeValue;
import consulo.project.Project;
import consulo.project.ui.wm.ToolWindowFactory;
import consulo.rust.icon.RustIconGroup;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.ex.awt.SimpleToolWindowPanel;
import consulo.ui.ex.content.Content;
import consulo.ui.ex.content.ContentFactory;
import consulo.ui.ex.toolWindow.ToolWindow;
import consulo.ui.ex.toolWindow.ToolWindowAnchor;
import consulo.ui.image.Image;
import jakarta.annotation.Nonnull;
import org.rust.cargo.project.model.CargoProjectServiceUtil;

@ExtensionImpl
public class CargoToolWindowFactory implements ToolWindowFactory, DumbAware {
    public static final String ID = "Cargo";

    @Nonnull
    @Override
    public String getId() {
        return ID;
    }

    @Nonnull
    @Override
    public ToolWindowAnchor getAnchor() {
        return ToolWindowAnchor.RIGHT;
    }

    @Nonnull
    @Override
    public Image getIcon() {
        return RustIconGroup.cargo();
    }

    @Nonnull
    @Override
    public LocalizeValue getDisplayName() {
        return LocalizeValue.localizeTODO("Cargo");
    }

    @RequiredUIAccess
    @Override
    public void createToolWindowContent(@Nonnull Project project, @Nonnull ToolWindow toolWindow) {
        CargoProjectServiceUtil.guessAndSetupRustProject(project);

        CargoToolWindow cargoToolWindow = new CargoToolWindow(project);
        SimpleToolWindowPanel toolWindowPanel = new SimpleToolWindowPanel(true, false) {
            @Override
            public void uiDataSnapshot(@Nonnull DataSink sink) {
                super.uiDataSnapshot(sink);
                sink.set(CargoToolWindow.SELECTED_CARGO_PROJECT, cargoToolWindow.getSelectedProject());
                sink.set(PlatformDataKeys.TREE_EXPANDER, cargoToolWindow.treeExpander);
            }
        };
        toolWindowPanel.setToolbar(cargoToolWindow.toolbar.getComponent());
        cargoToolWindow.toolbar.setTargetComponent(toolWindowPanel);
        toolWindowPanel.setContent(cargoToolWindow.content);

        Content tab = ContentFactory.getInstance().createContent(toolWindowPanel, "", false);
        toolWindow.getContentManager().addContent(tab);
    }
}
