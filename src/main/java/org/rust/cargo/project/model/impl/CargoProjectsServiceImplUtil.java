/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.cargo.project.model.impl;

import org.jetbrains.annotations.NotNull;
import org.rust.cargo.project.model.CargoProject;
import org.rust.cargo.project.model.CargoProjectsService;
import org.rust.cargo.project.workspace.CargoWorkspace;

import java.util.ArrayList;
import java.util.List;

public final class CargoProjectsServiceImplUtil {
    private CargoProjectsServiceImplUtil() {
    }

    @NotNull
    public static List<CargoWorkspace.Target> getAllTargets(@NotNull CargoProjectsService service) {
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

    @NotNull
    public static List<CargoWorkspace.Package> getAllPackages(@NotNull CargoProjectsService service) {
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
