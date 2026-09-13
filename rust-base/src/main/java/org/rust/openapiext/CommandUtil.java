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
import consulo.localize.LocalizeValue;

/**
 * Bridge class delegating to {@link OpenApiUtil}.
 */
public final class CommandUtil {
    private CommandUtil() {
    }

    public static <T> T runWriteCommandAction(@Nonnull Project project,
                                               @Nonnull String commandName,
                                               @Nonnull PsiFile[] files,
                                               @Nonnull Supplier<T> command) {
        return OpenApiUtil.runWriteCommandAction(project, commandName, files, command);
    }

    public static void runWriteCommandAction(@Nonnull Project project,
                                              @Nonnull String commandName,
                                              @Nonnull Runnable command) {
        OpenApiUtil.runWriteCommandAction(project, commandName, new PsiFile[0], () -> { command.run(); return null; });
    }

    public static void runWriteCommandAction(@Nonnull Project project,
                                              @Nonnull consulo.localize.LocalizeValue commandName,
                                              @Nonnull Runnable command) {
        runWriteCommandAction(project, commandName.get(), command);
    }

    public static void runWriteCommandAction(@Nonnull Project project,
                                              @Nonnull String commandName,
                                              @Nonnull PsiFile file,
                                              @Nonnull Runnable command) {
        OpenApiUtil.runWriteCommandAction(project, commandName, new PsiFile[]{file}, () -> { command.run(); return null; });
    }

    public static void runUndoTransparentWriteCommandAction(@Nonnull Project project, @Nonnull Runnable command) {
        OpenApiUtil.runUndoTransparentWriteCommandAction(project, command);
    }

    public static void checkWriteAccessNotAllowed() {
        OpenApiUtil.checkWriteAccessNotAllowed();
    }
}
