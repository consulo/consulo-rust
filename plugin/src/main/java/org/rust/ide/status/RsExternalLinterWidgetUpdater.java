/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.ide.status;

import consulo.annotation.component.ComponentScope;
import consulo.annotation.component.TopicImpl;
import consulo.project.Project;
import consulo.project.ui.wm.StatusBarWidgetsManager;
import consulo.ui.UIAccess;
import jakarta.annotation.Nonnull;
import org.rust.cargo.api.model.CargoProject;
import org.rust.cargo.api.model.CargoProjectsService;

import java.util.Collection;
import org.rust.cargo.api.model.CargoProjectsListener;

@TopicImpl(ComponentScope.PROJECT)
public class RsExternalLinterWidgetUpdater implements CargoProjectsListener {
    @jakarta.annotation.Nullable
    private final Project project;

    public RsExternalLinterWidgetUpdater() {
        this(null);
    }

    public RsExternalLinterWidgetUpdater(@jakarta.annotation.Nullable Project project) {
        this.project = project;
    }

    @Override
    public void cargoProjectsUpdated(@Nonnull CargoProjectsService service, @Nonnull Collection<CargoProject> projects) {
        // The widget is only available while at least one Cargo project is attached, so ask the manager
        // to re-evaluate it whenever the set of projects changes.
        Project target = project != null ? project : service.getProject();
        StatusBarWidgetsManager.getInstance(target).updateWidget(RsExternalLinterWidgetFactory.class, UIAccess.get());
    }
}
