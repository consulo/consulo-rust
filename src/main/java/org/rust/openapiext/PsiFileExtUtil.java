/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.openapiext;

import com.intellij.openapi.editor.Document;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiFile;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Delegates to methods in {@link OpenApiUtil}.
 */
public final class PsiFileExtUtil {
    private PsiFileExtUtil() {
    }

    @Nullable
    public static PsiFile toPsiFile(@NotNull VirtualFile file, @NotNull Project project) {
        return OpenApiUtil.toPsiFile(file, project);
    }

    @Nullable
    public static PsiFile toPsiFile(@NotNull Document document, @NotNull Project project) {
        return OpenApiUtil.toPsiFile(document, project);
    }
}
