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
public final class CommandUtil {
    private CommandUtil() {
    }

    public static <T> T runWriteCommandAction(@NotNull Project project,
                                               @NotNull String commandName,
                                               @NotNull PsiFile[] files,
                                               @NotNull Supplier<T> command) {
        return OpenApiUtil.runWriteCommandAction(project, commandName, files, command);
    }

    public static void runWriteCommandAction(@NotNull Project project,
                                              @NotNull String commandName,
                                              @NotNull Runnable command) {
        OpenApiUtil.runWriteCommandAction(project, commandName, new PsiFile[0], () -> { command.run(); return null; });
    }

    public static void runWriteCommandAction(@NotNull Project project,
                                              @NotNull String commandName,
                                              @NotNull PsiFile file,
                                              @NotNull Runnable command) {
        OpenApiUtil.runWriteCommandAction(project, commandName, new PsiFile[]{file}, () -> { command.run(); return null; });
    }

    public static void runUndoTransparentWriteCommandAction(@NotNull Project project, @NotNull Runnable command) {
        OpenApiUtil.runUndoTransparentWriteCommandAction(project, command);
    }

    public static void checkWriteAccessNotAllowed() {
        OpenApiUtil.checkWriteAccessNotAllowed();
    }
}
