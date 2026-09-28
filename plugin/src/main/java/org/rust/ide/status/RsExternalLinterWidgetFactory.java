/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.ide.status;

import consulo.annotation.component.ExtensionImpl;
import consulo.project.Project;
import consulo.disposer.Disposer;
import consulo.project.ui.wm.StatusBar;
import consulo.project.ui.wm.StatusBarWidget;
import consulo.project.ui.wm.StatusBarWidgetFactory;
import jakarta.annotation.Nonnull;
import org.rust.RsBundle;
import org.rust.cargo.runconfig.RunConfigUtil;

@ExtensionImpl
public class RsExternalLinterWidgetFactory implements StatusBarWidgetFactory {
    @Override
    @Nonnull
    public String getId() {
        return RsExternalLinterWidget.ID;
    }

    @Override
    @Nonnull
    public String getDisplayName() {
        return RsBundle.message("configurable.name.rust.external.linter");
    }

    /**
     * The widget is a Swing panel built on {@code consulo.ide.impl.idea...TextPanel.WithIconAndArrows},
     * so a frontend that does not ship ide-impl cannot show it. Reported here rather than left to fail
     * in {@link #createWidget}: the manager treats an unavailable factory as simply absent, while a
     * createWidget that throws propagates out of the status bar and fails the whole project open.
     */
    private static final boolean WIDGET_AVAILABLE = isWidgetClassPresent();

    @Override
    public boolean isAvailable(@Nonnull Project project) {
        return WIDGET_AVAILABLE && RunConfigUtil.hasCargoProject(project);
    }

    private static boolean isWidgetClassPresent() {
        try {
            Class.forName(
                "consulo.ide.impl.idea.openapi.wm.impl.status.TextPanel$WithIconAndArrows",
                false,
                RsExternalLinterWidgetFactory.class.getClassLoader()
            );
            return true;
        }
        catch (ClassNotFoundException | LinkageError absent) {
            return false;
        }
    }

    @Override
    @Nonnull
    public StatusBarWidget createWidget(@Nonnull Project project) {
        return new RsExternalLinterWidget(project);
    }

    @Override
    public void disposeWidget(@Nonnull StatusBarWidget widget) {
        Disposer.dispose(widget);
    }

    @Override
    public boolean canBeEnabledOn(@Nonnull StatusBar statusBar) {
        return true;
    }
}
