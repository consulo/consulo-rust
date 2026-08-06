/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.openapiext;

import consulo.dataContext.DataContext;
import consulo.document.Document;
import consulo.codeEditor.Editor;
import consulo.application.progress.ProgressIndicator;
import consulo.project.Project;
import consulo.virtualFileSystem.VirtualFile;
import consulo.language.psi.PsiElement;
import consulo.language.psi.PsiFile;
import consulo.language.psi.SmartPsiElementPointer;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

import java.nio.file.Path;
import java.util.function.Supplier;

/**
 * Bridge class delegating to {@link PsiExt}.
 */
public final class PsiExtUtil {
    private PsiExtUtil() {
    }

    public static void forEachChild(@Nonnull PsiElement element, @Nonnull java.util.function.Consumer<PsiElement> action) {
        PsiExt.forEachChild(element, action);
    }
}
