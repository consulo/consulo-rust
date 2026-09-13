/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.cargo.api.model;


import consulo.virtualFileSystem.util.VirtualFileUtil;
import consulo.project.Project;
import consulo.virtualFileSystem.VirtualFile;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import org.rust.cargo.api.workspace.CargoWorkspace;

/**
 * Utility class for CargoProjectsService convenience methods.
 * Delegates to {@link CargoProjectServiceKt} where applicable.
 */
public final class CargoProjectsUtil {
    private CargoProjectsUtil() {
    }

    @Nonnull
    public static CargoProjectsService getCargoProjects(@Nonnull Project project) {
        return CargoProjectsService.getInstance(project);
    }

    public static boolean isGeneratedFile(@Nonnull Project project, @Nonnull VirtualFile file) {
        return isGeneratedFile(CargoProjectsService.getInstance(project), file);
    }

    @Nullable
    public static CargoWorkspace.Package findPackageForFile(@Nonnull Project project, @Nonnull VirtualFile file) {
        return getCargoProjects(project).findPackageForFile(file);
    }

    public static boolean isGeneratedFile(@Nonnull CargoProjectsService service, @Nonnull VirtualFile file) {
        CargoWorkspace.Package pkg = service.findPackageForFile(file);
        if (pkg == null) return false;
        VirtualFile outDir = pkg.getOutDir();
        if (outDir == null) return false;
        return VirtualFileUtil.isAncestor(outDir, file, false);
    }
}
