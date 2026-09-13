/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.toml.crates.local;

import consulo.annotation.component.ComponentScope;
import consulo.annotation.component.TopicImpl;
import jakarta.annotation.Nonnull;
import org.rust.cargo.api.model.CargoProject;
import org.rust.cargo.api.model.CargoProjectsService;
import org.rust.experiments.RsExperiments;
import org.rust.openapiext.OpenApiUtil;

import java.util.Collection;
import org.rust.cargo.api.model.CargoProjectsListener;

/**
 * Loads the crates index and schedules its update once a Cargo project is available.
 */
@TopicImpl(ComponentScope.PROJECT)
public class CratesLocalIndexWaker implements CargoProjectsListener {

    @Override
    public void cargoProjectsUpdated(@Nonnull CargoProjectsService service,
                                      @Nonnull Collection<CargoProject> projects) {
        if (!projects.isEmpty() && OpenApiUtil.isFeatureEnabled(RsExperiments.CRATES_LOCAL_INDEX)) {
            CratesLocalIndexService index = CratesLocalIndexService.getInstance();
            if (index instanceof CratesLocalIndexServiceImpl) {
                ((CratesLocalIndexServiceImpl) index).recoverIfNeeded();
            }
            CratesLocalIndexUpdater.getInstance().updateCratesIoGitIndex();
        }
    }
}
