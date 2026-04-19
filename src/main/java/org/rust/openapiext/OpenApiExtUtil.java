/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.openapiext;

import com.intellij.openapi.editor.Document;
import com.intellij.openapi.options.Configurable;
import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.project.DumbService;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.Computable;
import com.intellij.openapi.util.Key;
import com.intellij.openapi.util.Pair;
import com.intellij.openapi.util.UserDataHolder;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiFile;
import org.jetbrains.annotations.NotNull;

import java.lang.ref.SoftReference;
import java.nio.file.Path;
import java.util.function.Supplier;

/**
 * Bridge class delegating to {@link OpenApiUtil}.
 */
public final class OpenApiExtUtil {
    private OpenApiExtUtil() {
    }

    public static PsiFile toPsiFile(@NotNull VirtualFile file, @NotNull Project project) {
        return OpenApiUtil.toPsiFile(file, project);
    }

    public static PsiFile toPsiFile(@NotNull Document document, @NotNull Project project) {
        return OpenApiUtil.toPsiFile(document, project);
    }

    public static Path getPathAsPath(@NotNull VirtualFile file) {
        return OpenApiUtil.getPathAsPath(file);
    }

    public static void checkIsSmartMode(@NotNull Project project) {
        OpenApiUtil.checkIsSmartMode(project);
    }

    public static <T> T executeUnderProgress(@NotNull ProgressIndicator indicator, @NotNull Supplier<T> action) {
        return OpenApiUtil.executeUnderProgress(indicator, action);
    }

    public static boolean isDispatchThread() {
        return OpenApiUtil.isDispatchThread();
    }

    public static <T> T executeUnderProgressWithWriteActionPriorityWithRetries(@NotNull ProgressIndicator indicator,
        @NotNull java.util.function.Function<ProgressIndicator, T> action) {
        return OpenApiUtil.executeUnderProgressWithWriteActionPriorityWithRetries(indicator, action);
    }

    public static <T extends Configurable> void showSettingsDialog(@NotNull Project project, @NotNull Class<T> configurableClass) {
        OpenApiUtil.showSettingsDialog(project, configurableClass);
    }

    public static <T> T recursionGuard(@NotNull Object key, @NotNull Computable<T> block, boolean memoize) {
        return OpenApiUtil.recursionGuard(key, block, memoize);
    }

    public static <T> T recursionGuard(@NotNull Object key, @NotNull Computable<T> block) {
        return OpenApiUtil.recursionGuard(key, block);
    }

    public static <T> T runWriteCommandAction(@NotNull Project project,
                                               @NotNull String commandName,
                                               @NotNull PsiFile[] files,
                                               @NotNull Supplier<T> command) {
        return OpenApiUtil.runWriteCommandAction(project, commandName, files, command);
    }

    public static void checkReadAccessAllowed() {
        OpenApiUtil.checkReadAccessAllowed();
    }

    public static void checkWriteAccessAllowed() {
        OpenApiUtil.checkWriteAccessAllowed();
    }

    public static void testAssert(@NotNull Supplier<Boolean> action) {
        OpenApiUtil.testAssert(action);
    }

    public static void testAssert(@NotNull Supplier<Boolean> action, @NotNull Supplier<Object> lazyMessage) {
        OpenApiUtil.testAssert(action, lazyMessage);
    }

    public static <T, D> T getCachedOrCompute(@NotNull UserDataHolder dataHolder,
        @NotNull Key<SoftReference<Pair<T, D>>> key,
        @NotNull D dependency,
        @NotNull Supplier<T> provider) {
        return OpenApiUtil.getCachedOrCompute(dataHolder, key, dependency, provider);
    }

    public static <T> T getOrPut(@NotNull UserDataHolder holder, @NotNull Key<T> key, @NotNull Supplier<T> defaultValue) {
        return OpenApiUtil.getOrPut(holder, key, defaultValue);
    }

    public static <T> T runReadActionInSmartMode(@NotNull DumbService dumbService, @NotNull Supplier<T> action) {
        return OpenApiUtil.runReadActionInSmartMode(dumbService, action);
    }

    public static <T> T computeInReadActionWithWriteActionPriority(@NotNull ProgressIndicator indicator,
                                                                    @NotNull Supplier<T> action) {
        return OpenApiUtil.computeInReadActionWithWriteActionPriority(indicator, action);
    }

    public static boolean isUnitTestMode() {
        return OpenApiUtil.isUnitTestMode();
    }
}
