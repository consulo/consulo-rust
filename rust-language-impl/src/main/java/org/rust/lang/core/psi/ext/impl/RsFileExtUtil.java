/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.lang.core.psi.ext.impl;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import org.rust.cargo.api.workspace.CargoWorkspace;
import org.rust.lang.core.psi.*;

import java.util.List;
import org.rust.lang.core.psi.impl.*;
import org.rust.lang.core.psi.ext.*;

public final class RsFileExtUtil {
    private RsFileExtUtil() {}

    public static boolean isCrateRoot(@Nonnull RsFile file) {
        return file.isCrateRoot();
    }

    @Nullable
    public static CargoWorkspace.Package getContainingCargoPackage(@Nonnull RsFile file) {
        return RsElementUtil.getContainingCargoPackage(file);
    }

    @Nonnull
    public static List<RsModDeclItem> getDeclarations(@Nonnull RsFile file) {
        return file.getDeclarations();
    }
}
