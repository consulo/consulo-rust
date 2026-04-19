/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.lang.core.macros;

import org.jetbrains.annotations.NotNull;
import org.rust.cargo.project.model.CargoProject;
import org.rust.cargo.project.model.CargoProjectsService;

import java.util.Collection;

/**
 * Ensures that {@link MacroExpansionManager} service is loaded when {@link CargoProjectsService} is initialized.
 * {@link MacroExpansionManager} should be loaded in order to add expansion directory to the index via
 * RsIndexableSetContributor.
 */
public class MacroExpansionManagerWaker implements CargoProjectsService.CargoProjectsListener {
    @Override
    public void cargoProjectsUpdated(@NotNull CargoProjectsService service, @NotNull Collection<CargoProject> projects) {
        if (!projects.isEmpty()) {
            MacroExpansionManagerUtil.getMacroExpansionManager(service.getProject());
        }
    }
}
