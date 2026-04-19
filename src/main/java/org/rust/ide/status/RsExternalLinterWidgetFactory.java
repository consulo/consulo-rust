/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.ide.status;

import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.Disposer;
import com.intellij.openapi.wm.StatusBar;
import com.intellij.openapi.wm.StatusBarWidget;
import com.intellij.openapi.wm.StatusBarWidgetFactory;
import org.jetbrains.annotations.NotNull;
import org.rust.RsBundle;
import org.rust.cargo.runconfig.RunConfigUtil;

public class RsExternalLinterWidgetFactory implements StatusBarWidgetFactory {
    @Override
    @NotNull
    public String getId() {
        return RsExternalLinterWidget.ID;
    }

    @Override
    @NotNull
    public String getDisplayName() {
        return RsBundle.message("configurable.name.rust.external.linter");
    }

    @Override
    public boolean isAvailable(@NotNull Project project) {
        return RunConfigUtil.hasCargoProject(project);
    }

    @Override
    @NotNull
    public StatusBarWidget createWidget(@NotNull Project project) {
        return new RsExternalLinterWidget(project);
    }

    @Override
    public void disposeWidget(@NotNull StatusBarWidget widget) {
        Disposer.dispose(widget);
    }

    @Override
    public boolean canBeEnabledOn(@NotNull StatusBar statusBar) {
        return true;
    }
}
