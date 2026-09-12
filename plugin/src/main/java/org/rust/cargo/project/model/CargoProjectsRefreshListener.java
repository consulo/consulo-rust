/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.cargo.project.model;

import consulo.annotation.component.ComponentScope;
import consulo.annotation.component.TopicAPI;
import jakarta.annotation.Nonnull;

@TopicAPI(value = ComponentScope.PROJECT)
public interface CargoProjectsRefreshListener {
    void onRefreshStarted();

    void onRefreshFinished(@Nonnull CargoProjectsService.CargoRefreshStatus status);
}
