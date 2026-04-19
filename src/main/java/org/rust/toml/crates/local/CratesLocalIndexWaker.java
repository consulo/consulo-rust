/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.toml.crates.local;

import org.jetbrains.annotations.NotNull;
import org.rust.cargo.project.model.CargoProject;
import org.rust.cargo.project.model.CargoProjectsService;
import org.rust.ide.experiments.RsExperiments;
import org.rust.openapiext.OpenApiUtil;

import java.util.Collection;

public class CratesLocalIndexWaker implements CargoProjectsService.CargoProjectsListener {

    @Override
    public void cargoProjectsUpdated(@NotNull CargoProjectsService service,
                                      @NotNull Collection<CargoProject> projects) {
        if (!projects.isEmpty() && OpenApiUtil.isFeatureEnabled(RsExperiments.CRATES_LOCAL_INDEX)) {
            CratesLocalIndexService index = CratesLocalIndexService.getInstance();
            if (index instanceof CratesLocalIndexServiceImpl) {
                ((CratesLocalIndexServiceImpl) index).recoverIfNeeded();
            }
            CratesLocalIndexUpdater.getInstance().updateCratesIoGitIndex();
        }
    }
}
