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

import java.nio.file.Path;

/**
 * Delegates to methods in {@link OpenApiUtil}.
 */
public final class VirtualFileExtUtil {
    private VirtualFileExtUtil() {
    }

    @Nullable
    public static VirtualFile findFileByMaybeRelativePath(@Nonnull VirtualFile base, @Nonnull String path) {
        return OpenApiUtil.findFileByMaybeRelativePath(base, path);
    }

    @Nonnull
    public static Path getPathAsPath(@Nonnull VirtualFile file) {
        return OpenApiUtil.getPathAsPath(file);
    }

    @Nonnull
    public static Path pathAsPath(@Nonnull VirtualFile file) {
        return OpenApiUtil.getPathAsPath(file);
    }

    @Nullable
    public static PsiFile toPsiFile(@Nonnull VirtualFile file, @Nonnull Project project) {
        return OpenApiUtil.toPsiFile(file, project);
    }

    @Nullable
    public static Document getDocument(@Nonnull VirtualFile file) {
        return OpenApiUtil.getDocument(file);
    }

    public static int getFileId(@Nonnull VirtualFile file) {
        return OpenApiUtil.getFileId(file);
    }
}
