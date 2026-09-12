/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.cargo.project.model;

import consulo.annotation.component.ComponentScope;
import consulo.annotation.component.TopicAPI;
import jakarta.annotation.Nonnull;

import java.util.Collection;

@FunctionalInterface
@TopicAPI(value = ComponentScope.PROJECT)
public interface CargoProjectsListener {
    void cargoProjectsUpdated(@Nonnull CargoProjectsService service, @Nonnull Collection<CargoProject> projects);
}
