/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.lang.core.macros;

import consulo.annotation.component.ComponentScope;
import consulo.annotation.component.TopicImpl;
import jakarta.annotation.Nonnull;
import org.rust.cargo.project.model.CargoProject;
import org.rust.cargo.project.model.CargoProjectsService;

import java.util.Collection;
import org.rust.cargo.project.model.CargoProjectsListener;

/**
 * Ensures that {@link MacroExpansionManager} service is loaded when {@link CargoProjectsService} is initialized.
 * {@link MacroExpansionManager} should be loaded in order to add expansion directory to the index via
 * RsIndexableSetContributor.
 */
@TopicImpl(ComponentScope.PROJECT)
public class MacroExpansionManagerWaker implements CargoProjectsListener {
    @Override
    public void cargoProjectsUpdated(@Nonnull CargoProjectsService service, @Nonnull Collection<CargoProject> projects) {
        if (!projects.isEmpty()) {
            MacroExpansionManagerUtil.getMacroExpansionManager(service.getProject());
        }
    }
}
