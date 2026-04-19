/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.ide.status;

import com.intellij.openapi.project.Project;
import com.intellij.openapi.wm.impl.status.widget.StatusBarWidgetsManager;
import org.jetbrains.annotations.NotNull;
import org.rust.cargo.project.model.CargoProject;
import org.rust.cargo.project.model.CargoProjectsService;

import java.util.Collection;

public class RsExternalLinterWidgetUpdater implements CargoProjectsService.CargoProjectsListener {
    @NotNull
    private final Project project;

    public RsExternalLinterWidgetUpdater(@NotNull Project project) {
        this.project = project;
    }

    @Override
    public void cargoProjectsUpdated(@NotNull CargoProjectsService service, @NotNull Collection<CargoProject> projects) {
        StatusBarWidgetsManager manager = project.getService(StatusBarWidgetsManager.class);
        manager.updateWidget(RsExternalLinterWidgetFactory.class);
    }
}
