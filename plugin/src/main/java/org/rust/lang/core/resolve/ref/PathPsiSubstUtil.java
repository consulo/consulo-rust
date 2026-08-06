/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.lang.core.resolve.ref;

import jakarta.annotation.Nonnull;
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

    @Nonnull
    public static RsPsiSubstitution pathPsiSubst(@Nonnull RsPath path, @Nonnull RsGenericDeclaration declaration) {
        return pathPsiSubst(path, declaration, RsGenericDeclarationUtil.getGenericParameters(declaration));
    }

    @Nonnull
    public static RsPsiSubstitution pathPsiSubst(
        @Nonnull RsPath path,
        @Nonnull RsGenericDeclaration declaration,
        @Nonnull List<?> genericParameters
    ) {
        // Stub: returns empty substitution
        return new RsPsiSubstitution();
    }
}
