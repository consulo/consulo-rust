/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.openapiext;

import com.intellij.openapi.actionSystem.DataContext;
import com.intellij.openapi.editor.Document;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.intellij.psi.SmartPsiElementPointer;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.nio.file.Path;
import java.util.function.Supplier;

/**
 * Bridge class delegating to {@link OpenApiUtil}.
 */
public final class DocumentExtUtil {
    private DocumentExtUtil() {
    }

    public static Document getDocument(@NotNull VirtualFile file) {
        return OpenApiUtil.getDocument(file);
    }

    public static Document getDocument(@NotNull PsiFile file) {
        return OpenApiUtil.getDocument(file);
    }
}
