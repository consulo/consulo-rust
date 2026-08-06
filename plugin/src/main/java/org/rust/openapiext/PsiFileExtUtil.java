/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.openapiext;

import consulo.document.Document;
import consulo.project.Project;
import consulo.virtualFileSystem.VirtualFile;
import consulo.language.psi.PsiFile;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

/**
 * Delegates to methods in {@link OpenApiUtil}.
 */
public final class PsiFileExtUtil {
    private PsiFileExtUtil() {
    }

    @Nullable
    public static PsiFile toPsiFile(@Nonnull VirtualFile file, @Nonnull Project project) {
        return OpenApiUtil.toPsiFile(file, project);
    }

    @Nullable
    public static PsiFile toPsiFile(@Nonnull Document document, @Nonnull Project project) {
        return OpenApiUtil.toPsiFile(document, project);
    }
}
