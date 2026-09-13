/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.lang.core.resolve;

import consulo.project.Project;
import consulo.language.psi.PsiElement;
import consulo.util.lang.Pair;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import org.rust.cargo.api.workspace.CargoWorkspace;

/**
 * Delegates to methods in {@link NameResolutionKt}.
 */
public final class ResolveStringPathUtil {
    private ResolveStringPathUtil() {
    }

    @Nullable
    public static Pair<PsiElement, CargoWorkspace.Package> resolveStringPath(
        @Nonnull String path,
        @Nonnull CargoWorkspace workspace,
        @Nonnull Project project
    ) {
        var result = NameResolutionUtil.resolveStringPath(path, workspace, project);
        if (result == null) return null;
        return new Pair<>(result.getFirst(), result.getSecond());
    }
}
