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

import java.nio.file.Path;

/**
 * Delegates to methods in {@link OpenApiUtil}.
 */
public final class VirtualFileExtUtil {
    private VirtualFileExtUtil() {
    }

    @Nullable
    public static VirtualFile findFileByMaybeRelativePath(@NotNull VirtualFile base, @NotNull String path) {
        return OpenApiUtil.findFileByMaybeRelativePath(base, path);
    }

    @NotNull
    public static Path getPathAsPath(@NotNull VirtualFile file) {
        return OpenApiUtil.getPathAsPath(file);
    }

    @NotNull
    public static Path pathAsPath(@NotNull VirtualFile file) {
        return OpenApiUtil.getPathAsPath(file);
    }

    @Nullable
    public static PsiFile toPsiFile(@NotNull VirtualFile file, @NotNull Project project) {
        return OpenApiUtil.toPsiFile(file, project);
    }

    @Nullable
    public static Document getDocument(@NotNull VirtualFile file) {
        return OpenApiUtil.getDocument(file);
    }

    public static int getFileId(@NotNull VirtualFile file) {
        return OpenApiUtil.getFileId(file);
    }
}
