/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.openapiext;

import consulo.document.Document;
import consulo.configurable.Configurable;
import consulo.application.progress.ProgressIndicator;
import consulo.project.DumbService;
import consulo.project.Project;
import consulo.application.util.function.Computable;
import consulo.util.dataholder.Key;
import consulo.util.lang.Pair;
import consulo.util.dataholder.UserDataHolder;
import consulo.virtualFileSystem.VirtualFile;
import consulo.language.psi.PsiFile;
import jakarta.annotation.Nonnull;

import java.lang.ref.SoftReference;
import java.nio.file.Path;
import java.util.function.Supplier;

/**
 * Bridge class delegating to {@link OpenApiUtil}.
 */
public final class OpenApiExtUtil {
    private OpenApiExtUtil() {
    }

    public static PsiFile toPsiFile(@Nonnull VirtualFile file, @Nonnull Project project) {
        return OpenApiUtil.toPsiFile(file, project);
    }

    public static PsiFile toPsiFile(@Nonnull Document document, @Nonnull Project project) {
        return OpenApiUtil.toPsiFile(document, project);
    }

    public static Path getPathAsPath(@Nonnull VirtualFile file) {
        return OpenApiUtil.getPathAsPath(file);
    }

    public static void checkIsSmartMode(@Nonnull Project project) {
        OpenApiUtil.checkIsSmartMode(project);
    }

    public static <T> T executeUnderProgress(@Nonnull ProgressIndicator indicator, @Nonnull Supplier<T> action) {
        return OpenApiUtil.executeUnderProgress(indicator, action);
    }

    public static boolean isDispatchThread() {
        return OpenApiUtil.isDispatchThread();
    }

    public static <T> T executeUnderProgressWithWriteActionPriorityWithRetries(@Nonnull ProgressIndicator indicator,
        @Nonnull java.util.function.Function<ProgressIndicator, T> action) {
        return OpenApiUtil.executeUnderProgressWithWriteActionPriorityWithRetries(indicator, action);
    }

    public static <T extends Configurable> void showSettingsDialog(@Nonnull Project project, @Nonnull Class<T> configurableClass) {
        OpenApiUtil.showSettingsDialog(project, configurableClass);
    }

    public static <T> T recursionGuard(@Nonnull Object key, @Nonnull Computable<T> block, boolean memoize) {
        return OpenApiUtil.recursionGuard(key, block, memoize);
    }

    public static <T> T recursionGuard(@Nonnull Object key, @Nonnull Computable<T> block) {
        return OpenApiUtil.recursionGuard(key, block);
    }

    public static <T> T runWriteCommandAction(@Nonnull Project project,
                                               @Nonnull String commandName,
                                               @Nonnull PsiFile[] files,
                                               @Nonnull Supplier<T> command) {
        return OpenApiUtil.runWriteCommandAction(project, commandName, files, command);
    }

    public static void checkReadAccessAllowed() {
        OpenApiUtil.checkReadAccessAllowed();
    }

    public static void checkWriteAccessAllowed() {
        OpenApiUtil.checkWriteAccessAllowed();
    }

    public static void testAssert(@Nonnull Supplier<Boolean> action) {
        OpenApiUtil.testAssert(action);
    }

    public static void testAssert(@Nonnull Supplier<Boolean> action, @Nonnull Supplier<Object> lazyMessage) {
        OpenApiUtil.testAssert(action, lazyMessage);
    }

    public static <T, D> T getCachedOrCompute(@Nonnull UserDataHolder dataHolder,
        @Nonnull Key<SoftReference<Pair<T, D>>> key,
        @Nonnull D dependency,
        @Nonnull Supplier<T> provider) {
        return OpenApiUtil.getCachedOrCompute(dataHolder, key, dependency, provider);
    }

    public static <T> T getOrPut(@Nonnull UserDataHolder holder, @Nonnull Key<T> key, @Nonnull Supplier<T> defaultValue) {
        return OpenApiUtil.getOrPut(holder, key, defaultValue);
    }

    public static <T> T runReadActionInSmartMode(@Nonnull DumbService dumbService, @Nonnull Supplier<T> action) {
        return OpenApiUtil.runReadActionInSmartMode(dumbService, action);
    }

    public static <T> T computeInReadActionWithWriteActionPriority(@Nonnull ProgressIndicator indicator,
                                                                    @Nonnull Supplier<T> action) {
        return OpenApiUtil.computeInReadActionWithWriteActionPriority(indicator, action);
    }

    public static boolean isUnitTestMode() {
        return OpenApiUtil.isUnitTestMode();
    }
}
