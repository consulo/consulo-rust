/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.lang.core.resolve.ref;

import org.jetbrains.annotations.NotNull;
import org.rust.lang.core.psi.RsPath;
import org.rust.lang.core.psi.ext.RsGenericDeclaration;
import org.rust.lang.core.psi.ext.RsGenericDeclarationUtil;
import org.rust.lang.core.types.RsPsiSubstitution;

import java.util.List;

/**
 * Utility for computing PSI-level substitutions from paths.
 */
public final class PathPsiSubstUtil {
    private PathPsiSubstUtil() {
    }

    @NotNull
    public static RsPsiSubstitution pathPsiSubst(@NotNull RsPath path, @NotNull RsGenericDeclaration declaration) {
        return pathPsiSubst(path, declaration, RsGenericDeclarationUtil.getGenericParameters(declaration));
    }

    @NotNull
    public static RsPsiSubstitution pathPsiSubst(
        @NotNull RsPath path,
        @NotNull RsGenericDeclaration declaration,
        @NotNull List<?> genericParameters
    ) {
        // Stub: returns empty substitution
        return new RsPsiSubstitution();
    }
}
