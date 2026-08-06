/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.cargo.project.model;

import consulo.project.Project;
import consulo.virtualFileSystem.VirtualFile;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import org.rust.cargo.project.workspace.CargoWorkspace;

/**
 * Utility class for CargoProjectsService convenience methods.
 * Delegates to {@link CargoProjectServiceKt} where applicable.
 */
public final class CargoProjectsUtil {
    private CargoProjectsUtil() {
    }

    @Nonnull
    public static CargoProjectsService getCargoProjects(@Nonnull Project project) {
        return CargoProjectServiceUtil.getCargoProjects(project);
    }

    public static boolean isGeneratedFile(@Nonnull Project project, @Nonnull VirtualFile file) {
        return CargoProjectServiceUtil.isGeneratedFile(getCargoProjects(project), file);
    }

    @Nullable
    public static CargoWorkspace.Package findPackageForFile(@Nonnull Project project, @Nonnull VirtualFile file) {
        return getCargoProjects(project).findPackageForFile(file);
    }
}
