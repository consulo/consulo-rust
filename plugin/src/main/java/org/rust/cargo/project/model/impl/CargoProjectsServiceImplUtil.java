/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.cargo.project.model.impl;

import jakarta.annotation.Nonnull;
import org.rust.cargo.api.model.CargoProject;
import org.rust.cargo.api.model.CargoProjectsService;
import org.rust.cargo.api.workspace.CargoWorkspace;

import java.util.ArrayList;
import java.util.List;

public final class CargoProjectsServiceImplUtil {
    private CargoProjectsServiceImplUtil() {
    }

    @Nonnull
    public static List<CargoWorkspace.Target> getAllTargets(@Nonnull CargoProjectsService service) {
        List<CargoWorkspace.Target> result = new ArrayList<>();
        for (CargoProject project : service.getAllProjects()) {
            CargoWorkspace workspace = project.getWorkspace();
            if (workspace != null) {
                for (CargoWorkspace.Package pkg : workspace.getPackages()) {
                    result.addAll(pkg.getTargets());
                }
            }
        }
        return result;
    }

    @Nonnull
    public static List<CargoWorkspace.Package> getAllPackages(@Nonnull CargoProjectsService service) {
        List<CargoWorkspace.Package> result = new ArrayList<>();
        for (CargoProject project : service.getAllProjects()) {
            CargoWorkspace workspace = project.getWorkspace();
            if (workspace != null) {
                result.addAll(workspace.getPackages());
            }
        }
        return result;
    }
}
