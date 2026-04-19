/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.lang.core.psi.ext;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.rust.cargo.project.workspace.CargoWorkspace;
import org.rust.lang.core.psi.*;

import java.util.List;

public final class RsFileExtUtil {
    private RsFileExtUtil() {}

    public static boolean isCrateRoot(@NotNull RsFile file) {
        return file.isCrateRoot();
    }

    @Nullable
    public static CargoWorkspace.Package getContainingCargoPackage(@NotNull RsFile file) {
        return RsElementUtil.getContainingCargoPackage(file);
    }

    @NotNull
    public static List<RsModDeclItem> getDeclarations(@NotNull RsFile file) {
        return file.getDeclarations();
    }
}
