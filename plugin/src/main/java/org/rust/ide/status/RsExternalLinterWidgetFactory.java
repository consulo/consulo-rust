/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.ide.status;

import consulo.project.Project;
import consulo.disposer.Disposer;
import consulo.project.ui.wm.StatusBar;
import consulo.project.ui.wm.StatusBarWidget;
import consulo.project.ui.wm.StatusBarWidgetFactory;
import jakarta.annotation.Nonnull;
import org.rust.RsBundle;
import org.rust.cargo.runconfig.RunConfigUtil;

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

    @Override
    public boolean isAvailable(@Nonnull Project project) {
        return RunConfigUtil.hasCargoProject(project);
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
