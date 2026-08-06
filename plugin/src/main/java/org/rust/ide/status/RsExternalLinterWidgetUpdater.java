/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.ide.status;

import consulo.project.Project;
import jakarta.annotation.Nonnull;
import org.rust.cargo.project.model.CargoProject;
import org.rust.cargo.project.model.CargoProjectsService;

import java.util.Collection;

public class RsExternalLinterWidgetUpdater implements CargoProjectsService.CargoProjectsListener {
    @Nonnull
    private final Project project;

    public RsExternalLinterWidgetUpdater(@Nonnull Project project) {
        this.project = project;
    }

    @Override
    public void cargoProjectsUpdated(@Nonnull CargoProjectsService service, @Nonnull Collection<CargoProject> projects) {
        // TODO: Consulo StatusBarWidgetsManager equivalent not wired yet
    }
}
