/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.lang.core.resolve;

import com.intellij.openapi.project.Project;
import com.intellij.psi.PsiElement;
import com.intellij.openapi.util.Pair;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.rust.cargo.project.workspace.CargoWorkspace;

/**
 * Delegates to methods in {@link NameResolutionKt}.
 */
public final class ResolveStringPathUtil {
    private ResolveStringPathUtil() {
    }

    @Nullable
    public static Pair<PsiElement, CargoWorkspace.Package> resolveStringPath(
        @NotNull String path,
        @NotNull CargoWorkspace workspace,
        @NotNull Project project
    ) {
        var result = NameResolutionUtil.resolveStringPath(path, workspace, project);
        if (result == null) return null;
        return new Pair<>(result.getFirst(), result.getSecond());
    }
}
