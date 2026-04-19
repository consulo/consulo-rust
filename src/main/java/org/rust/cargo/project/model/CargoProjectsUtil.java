/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.cargo.project.model;

import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.rust.cargo.project.workspace.CargoWorkspace;

/**
 * Utility class for CargoProjectsService convenience methods.
 * Delegates to {@link CargoProjectServiceKt} where applicable.
 */
public final class CargoProjectsUtil {
    private CargoProjectsUtil() {
    }

    @NotNull
    public static CargoProjectsService getCargoProjects(@NotNull Project project) {
        return CargoProjectServiceUtil.getCargoProjects(project);
    }

    public static boolean isGeneratedFile(@NotNull Project project, @NotNull VirtualFile file) {
        return CargoProjectServiceUtil.isGeneratedFile(getCargoProjects(project), file);
    }

    @Nullable
    public static CargoWorkspace.Package findPackageForFile(@NotNull Project project, @NotNull VirtualFile file) {
        return getCargoProjects(project).findPackageForFile(file);
    }
}
