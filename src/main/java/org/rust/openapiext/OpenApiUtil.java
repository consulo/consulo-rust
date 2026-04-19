/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.openapiext;

import com.intellij.concurrency.SensitiveProgressWrapper;
import com.intellij.ide.plugins.IdeaPluginDescriptor;
import com.intellij.ide.plugins.PluginManagerCore;
import com.intellij.ide.ui.LafManager;
import com.intellij.ide.ui.laf.UIThemeBasedLookAndFeelInfo;
import com.intellij.openapi.Disposable;
import com.intellij.openapi.actionSystem.CommonDataKeys;
import com.intellij.openapi.actionSystem.DataContext;
import com.intellij.openapi.application.*;
import com.intellij.openapi.application.ex.ApplicationUtil;
import com.intellij.openapi.command.CommandProcessor;
import com.intellij.openapi.command.WriteCommandAction;
import com.intellij.openapi.editor.Document;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.editor.impl.TrailingSpacesStripper;
import com.intellij.openapi.extensions.PluginId;
import com.intellij.openapi.fileEditor.FileDocumentManager;
import com.intellij.openapi.fileEditor.impl.FileDocumentManagerImpl;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.module.ModuleManager;
import com.intellij.openapi.options.Configurable;
import com.intellij.openapi.options.ShowSettingsUtil;
import com.intellij.openapi.progress.EmptyProgressIndicator;
import com.intellij.openapi.progress.ProcessCanceledException;
import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.progress.ProgressManager;
import com.intellij.openapi.progress.util.AbstractProgressIndicatorExBase;
import com.intellij.openapi.progress.util.ProgressIndicatorUtils;
import com.intellij.openapi.project.DumbService;
import com.intellij.openapi.project.IndexNotReadyException;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.*;
import com.intellij.openapi.util.io.FileUtil;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.openapi.vfs.VfsUtil;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.openapi.vfs.VirtualFileWithId;
import com.intellij.openapi.wm.ex.ProgressIndicatorEx;
import com.intellij.psi.*;
import com.intellij.psi.impl.PsiDocumentManagerBase;
import com.intellij.psi.search.GlobalSearchScope;
import com.intellij.psi.stubs.StubIndex;
import com.intellij.psi.stubs.StubIndexKey;
import com.intellij.util.concurrency.AppExecutorUtil;
import com.intellij.util.ui.UIUtil;
import org.jdom.Element;
import org.jdom.input.SAXBuilder;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.rust.cargo.RustfmtWatcher;
import org.rust.ide.annotator.RsExternalLinterPass;

import java.io.ByteArrayInputStream;
import java.lang.ref.SoftReference;
import java.lang.reflect.Field;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.concurrent.Callable;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;

/**
 * Utility methods for working with the IntelliJ Open API.
 */
public final class OpenApiUtil {
    private OpenApiUtil() {
    }

    @NotNull
    public static final String PLUGIN_ID = "org.rust.lang";

    // --- Property-style accessors ---

    public static boolean isUnitTestMode() {
        return ApplicationManager.getApplication().isUnitTestMode();
    }

    public static boolean isHeadlessEnvironment() {
        return ApplicationManager.getApplication().isHeadlessEnvironment();
    }

    public static boolean isDispatchThread() {
        return ApplicationManager.getApplication().isDispatchThread();
    }

    public static boolean isInternal() {
        return ApplicationManager.getApplication().isInternal();
    }

    public static boolean isUnderDarkTheme() {
        Object lookAndFeel = LafManager.getInstance().getCurrentLookAndFeel();
        if (lookAndFeel instanceof UIThemeBasedLookAndFeelInfo) {
            UIThemeBasedLookAndFeelInfo themeInfo = (UIThemeBasedLookAndFeelInfo) lookAndFeel;
            if (themeInfo.getTheme().isDark()) return true;
        }
        return UIUtil.isUnderDarcula();
    }

    // --- Write command actions ---

    /**
     * Perform a write action for the provided project.
     *
     * @param project     the project
     * @param commandName the name of the action which will appear in the Undo/Redo stack
     * @param files       files modified by the action
     * @param command     the write action to perform
     */
    public static <T> T runWriteCommandAction(@NotNull Project project,
                                               @NotNull String commandName,
                                               @NotNull PsiFile[] files,
                                               @NotNull Supplier<T> command) {
        return WriteCommandAction.writeCommandAction(project, files)
            .withName(commandName)
            .compute(command::get);
    }

    /**
     * Overload without files parameter.
     */
    public static <T> T runWriteCommandAction(@NotNull Project project,
                                               @NotNull String commandName,
                                               @NotNull Supplier<T> command) {
        return runWriteCommandAction(project, commandName, new PsiFile[0], command);
    }

    /**
     * Overload accepting a Runnable (void action) without files parameter.
     */
    public static void runWriteCommandAction(@NotNull Project project,
                                              @NotNull String commandName,
                                              @NotNull Runnable command) {
        runWriteCommandAction(project, commandName, new PsiFile[0], () -> { command.run(); return null; });
    }

    /**
     * Modification of runUndoTransparentWriteAction which applies formatting to modified code.
     */
    public static void runUndoTransparentWriteCommandAction(@NotNull Project project, @NotNull Runnable command) {
        CommandProcessor.getInstance().runUndoTransparentAction(() ->
            WriteCommandAction.runWriteCommandAction(project, command)
        );
    }

    // --- Project utilities ---

    @NotNull
    public static Collection<Module> getModules(@NotNull Project project) {
        return Arrays.asList(ModuleManager.getInstance(project).getModules());
    }

    // --- Recursion guard ---

    @Nullable
    public static <T> T recursionGuard(@NotNull Object key, @NotNull Computable<T> block, boolean memoize) {
        return RecursionManager.doPreventingRecursion(key, memoize, block);
    }

    @Nullable
    public static <T> T recursionGuard(@NotNull Object key, @NotNull Computable<T> block) {
        return recursionGuard(key, block, true);
    }

    // --- Access checks ---

    public static void checkWriteAccessAllowed() {
        if (!ApplicationManager.getApplication().isWriteAccessAllowed()) {
            throw new IllegalStateException("Needs write action");
        }
    }

    public static void checkWriteAccessNotAllowed() {
        if (ApplicationManager.getApplication().isWriteAccessAllowed()) {
            throw new IllegalStateException("Write access should not be allowed");
        }
    }

    public static void checkReadAccessAllowed() {
        if (!ApplicationManager.getApplication().isReadAccessAllowed()) {
            throw new IllegalStateException("Needs read action");
        }
    }

    public static void checkReadAccessNotAllowed() {
        if (ApplicationManager.getApplication().isReadAccessAllowed()) {
            throw new IllegalStateException("Read access should not be allowed");
        }
    }

    public static void checkIsDispatchThread() {
        if (!ApplicationManager.getApplication().isDispatchThread()) {
            throw new IllegalStateException("Should be invoked on the Swing dispatch thread");
        }
    }

    public static void checkIsBackgroundThread() {
        if (ApplicationManager.getApplication().isDispatchThread()) {
            throw new IllegalStateException("Long running operation invoked on UI thread");
        }
    }

    public static void checkIsSmartMode(@NotNull Project project) {
        if (DumbService.getInstance(project).isDumb()) throw IndexNotReadyException.create();
    }

    public static void checkCommitIsNotInProgress(@NotNull Project project) {
        Application app = ApplicationManager.getApplication();
        if ((app.isUnitTestMode() || app.isInternal()) && app.isDispatchThread()) {
            if (((PsiDocumentManagerBase) PsiDocumentManager.getInstance(project)).isCommitInProgress()) {
                throw new IllegalStateException("Accessing indices during PSI event processing can lead to typing performance issues");
            }
        }
    }

    // --- VirtualFile utilities ---

    public static void fullyRefreshDirectory(@NotNull VirtualFile directory) {
        VfsUtil.markDirtyAndRefresh(false, true, true, directory);
    }

    @Nullable
    public static VirtualFile findFileByMaybeRelativePath(@NotNull VirtualFile base, @NotNull String path) {
        if (FileUtil.isAbsolute(path)) {
            return base.getFileSystem().findFileByPath(path);
        } else {
            return base.findFileByRelativePath(path);
        }
    }

    @NotNull
    public static Pair<VirtualFile, List<String>> findNearestExistingFile(@NotNull VirtualFile base, @NotNull String path) {
        VirtualFile file = base;
        List<String> segments = StringUtil.split(path, "/");
        for (int i = 0; i < segments.size(); i++) {
            VirtualFile child = file.findChild(segments.get(i));
            if (child == null) {
                return Pair.create(file, segments.subList(i, segments.size()));
            }
            file = child;
        }
        return Pair.create(file, Collections.emptyList());
    }

    @NotNull
    public static Path getPathAsPath(@NotNull VirtualFile file) {
        return Paths.get(file.getPath());
    }

    @Nullable
    public static PsiFile toPsiFile(@NotNull VirtualFile file, @NotNull Project project) {
        return PsiManager.getInstance(project).findFile(file);
    }

    @Nullable
    public static PsiDirectory toPsiDirectory(@NotNull VirtualFile file, @NotNull Project project) {
        return PsiManager.getInstance(project).findDirectory(file);
    }

    @Nullable
    public static PsiFile toPsiFile(@NotNull Document document, @NotNull Project project) {
        return PsiDocumentManager.getInstance(project).getPsiFile(document);
    }

    @Nullable
    public static VirtualFile getVirtualFile(@NotNull Document document) {
        return FileDocumentManager.getInstance().getFile(document);
    }

    @Nullable
    public static Document getDocument(@NotNull VirtualFile file) {
        return FileDocumentManager.getInstance().getDocument(file);
    }

    @Nullable
    public static Document getDocument(@NotNull PsiFile file) {
        return file.getViewProvider().getDocument();
    }

    public static int getFileId(@NotNull VirtualFile file) {
        return ((VirtualFileWithId) file).getId();
    }

    // --- StubIndex utilities ---

    @NotNull
    public static <Key, Psi extends PsiElement> Collection<Psi> getElements(
        @NotNull StubIndexKey<Key, Psi> indexKey,
        @NotNull Key key,
        @NotNull Project project,
        @Nullable GlobalSearchScope scope,
        @NotNull Class<Psi> requiredClass
    ) {
        return StubIndex.getElements(indexKey, key, project, scope, requiredClass);
    }

    /**
     * Callers should prefer the 5-parameter overload with an explicit class for type safety.
     */
    @SuppressWarnings("unchecked")
    @NotNull
    public static <Key, Psi extends PsiElement> Collection<Psi> getElements(
        @NotNull StubIndexKey<Key, Psi> indexKey,
        @NotNull Key key,
        @NotNull Project project,
        @Nullable GlobalSearchScope scope
    ) {
        return StubIndex.getElements(indexKey, key, project, scope, (Class<Psi>) PsiElement.class);
    }

    // --- XML utilities ---

    @NotNull
    public static String toXmlString(@NotNull Element element) {
        return JDOMUtil.writeElement(element);
    }

    @NotNull
    public static Element elementFromXmlString(@NotNull String xml) {
        try {
            return new SAXBuilder().build(new ByteArrayInputStream(xml.getBytes(StandardCharsets.UTF_8))).getRootElement();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    // --- Document save utilities ---

    public static void saveAllDocuments() {
        FileDocumentManager.getInstance().saveAllDocuments();
    }

    /**
     * Saves all documents "as they are" (without trailing spaces stripping),
     * but marks them for stripping later.
     */
    public static void saveAllDocumentsAsTheyAre(boolean reformatLater) {
        FileDocumentManager documentManager = FileDocumentManager.getInstance();
        RustfmtWatcher rustfmtWatcher = RustfmtWatcher.getInstance();
        rustfmtWatcher.withoutReformatting(() -> {
            for (Document document : documentManager.getUnsavedDocuments()) {
                documentManager.saveDocumentAsIs(document);
                stripDocumentLater(documentManager, document);
                if (reformatLater) rustfmtWatcher.reformatDocumentLater(document);
            }
        });
    }

    public static void saveAllDocumentsAsTheyAre() {
        saveAllDocumentsAsTheyAre(true);
    }

    private static boolean stripDocumentLater(@NotNull FileDocumentManager manager, @NotNull Document document) {
        if (!(manager instanceof FileDocumentManagerImpl)) return false;
        try {
            if (TRAILING_SPACES_STRIPPER_FIELD == null) return false;
            Object trailingSpacesStripper = TRAILING_SPACES_STRIPPER_FIELD.get(manager);
            if (!(trailingSpacesStripper instanceof TrailingSpacesStripper)) return false;

            if (DOCUMENTS_TO_STRIP_LATER_FIELD == null) return false;
            @SuppressWarnings("unchecked")
            Set<Document> documentsToStripLater = (Set<Document>) DOCUMENTS_TO_STRIP_LATER_FIELD.get(trailingSpacesStripper);
            if (documentsToStripLater == null) return false;
            return documentsToStripLater.add(document);
        } catch (IllegalAccessException e) {
            return false;
        }
    }

    @Nullable
    private static final Field TRAILING_SPACES_STRIPPER_FIELD = initFieldSafely(FileDocumentManagerImpl.class, "myTrailingSpacesStripper");

    @Nullable
    private static final Field DOCUMENTS_TO_STRIP_LATER_FIELD = initFieldSafely(TrailingSpacesStripper.class, "myDocumentsToStripLater");

    @Nullable
    private static Field initFieldSafely(@NotNull Class<?> clazz, @NotNull String fieldName) {
        try {
            Field field = clazz.getDeclaredField(fieldName);
            field.setAccessible(true);
            return field;
        } catch (Throwable e) {
            if (ApplicationManager.getApplication().isUnitTestMode()) throw new RuntimeException(e);
            return null;
        }
    }

    // --- Test assertions ---

    public static void testAssert(@NotNull Supplier<Boolean> action) {
        testAssert(action, () -> "Assertion failed");
    }

    public static void testAssert(@NotNull Supplier<Boolean> action, @NotNull Supplier<Object> lazyMessage) {
        if (isUnitTestMode() && !action.get()) {
            Object message = lazyMessage.get();
            throw new AssertionError(message);
        }
    }

    // --- Progress utilities ---

    @NotNull
    public static <T> T runWithCheckCanceled(@NotNull Supplier<T> callable) {
        try {
            return ApplicationUtil.runWithCheckCanceled(
                (Callable<T>) callable::get,
                ProgressManager.getInstance().getProgressIndicator()
            );
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public static <T> T computeWithCancelableProgress(@NotNull Project project,
                                                       @NotNull String title,
                                                       @NotNull Supplier<T> supplier) {
        if (isUnitTestMode()) {
            return supplier.get();
        }
        try {
            return ProgressManager.getInstance().runProcessWithProgressSynchronously(
                (ThrowableComputable<T, Exception>) supplier::get, title, true, project
            );
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public static boolean runWithCancelableProgress(@NotNull Project project,
                                                     @NotNull String title,
                                                     @NotNull Runnable process) {
        if (isUnitTestMode()) {
            process.run();
            return true;
        }
        return ProgressManager.getInstance().runProcessWithProgressSynchronously(process, title, true, project);
    }

    // --- UserDataHolder utilities ---

    @NotNull
    public static <T> T getOrPut(@NotNull UserDataHolder holder, @NotNull Key<T> key, @NotNull Supplier<T> defaultValue) {
        T data = holder.getUserData(key);
        if (data != null) return data;
        T value = defaultValue.get();
        holder.putUserData(key, value);
        return value;
    }

    // --- Plugin utilities ---

    @NotNull
    public static IdeaPluginDescriptor plugin() {
        IdeaPluginDescriptor descriptor = PluginManagerCore.getPlugin(PluginId.getId(PLUGIN_ID));
        assert descriptor != null : "Plugin descriptor not found for " + PLUGIN_ID;
        return descriptor;
    }

    // --- String utilities ---

    @NotNull
    public static String getEscaped(@NotNull String text) {
        return StringUtil.escapeXmlEntities(text);
    }

    @NotNull
    public static String escaped(@NotNull String text) {
        return StringUtil.escapeXmlEntities(text);
    }

    // --- Smart mode utilities ---

    public static <T> T runReadActionInSmartMode(@NotNull DumbService dumbService, @NotNull Supplier<T> action) {
        ProgressManager.checkCanceled();
        if (dumbService.getProject().isDisposed()) throw new ProcessCanceledException();
        return dumbService.runReadActionInSmartMode((Computable<T>) () -> {
            ProgressManager.checkCanceled();
            return action.get();
        });
    }

    // --- Write action priority utilities ---

    @NotNull
    public static <T> T executeUnderProgressWithWriteActionPriorityWithRetries(
        @NotNull ProgressIndicator indicator,
        @NotNull Function<ProgressIndicator, T> action
    ) {
        indicator.checkCanceled();
        if (isUnitTestMode() && ApplicationManager.getApplication().isReadAccessAllowed()) {
            return action.apply(indicator);
        } else {
            checkReadAccessNotAllowed();
        }
        @SuppressWarnings("unchecked")
        T[] result = (T[]) new Object[1];
        boolean success;
        do {
            SensitiveProgressWrapper wrappedIndicator = new SensitiveProgressWrapper(indicator);
            success = runWithWriteActionPriority(wrappedIndicator, () -> {
                result[0] = action.apply(wrappedIndicator);
            });
            if (!success) {
                indicator.checkCanceled();
                // wait for write action to complete
                ApplicationManager.getApplication().runReadAction(EmptyRunnable.getInstance());
            }
        } while (!success);
        return result[0];
    }

    public static boolean runWithWriteActionPriority(@NotNull ProgressIndicator indicator, @NotNull Runnable action) {
        return ProgressIndicatorUtils.runWithWriteActionPriority(action, indicator);
    }

    public static boolean runInReadActionWithWriteActionPriority(@NotNull ProgressIndicator indicator, @NotNull Runnable action) {
        return ProgressIndicatorUtils.runInReadActionWithWriteActionPriority(action, indicator);
    }

    @NotNull
    public static <T> T computeInReadActionWithWriteActionPriority(@NotNull ProgressIndicator indicator,
                                                                    @NotNull Supplier<T> action) {
        @SuppressWarnings("unchecked")
        T[] result = (T[]) new Object[1];
        boolean success = runInReadActionWithWriteActionPriority(indicator, () -> {
            result[0] = action.get();
        });
        if (!success) throw new ProcessCanceledException();
        return result[0];
    }

    public static <T> T executeUnderProgress(@NotNull ProgressIndicator indicator, @NotNull Supplier<T> action) {
        @SuppressWarnings("unchecked")
        T[] result = (T[]) new Object[1];
        ProgressManager.getInstance().executeProcessUnderProgress(() -> result[0] = action.get(), indicator);
        return result[0];
    }

    // --- Thread-safe progress indicator ---

    @NotNull
    public static ProgressIndicator toThreadSafeProgressIndicator(@NotNull ProgressIndicator indicator) {
        if (indicator instanceof ProgressIndicatorEx) {
            EmptyProgressIndicator threadSafeIndicator = new EmptyProgressIndicator();
            ((ProgressIndicatorEx) indicator).addStateDelegate(new AbstractProgressIndicatorExBase() {
                @Override
                public void cancel() {
                    threadSafeIndicator.cancel();
                }
            });
            return threadSafeIndicator;
        } else {
            return indicator;
        }
    }

    // --- Smart pointer utilities ---

    @NotNull
    public static <T extends PsiElement> SmartPsiElementPointer<T> createSmartPointer(@NotNull T element) {
        return SmartPointerManager.getInstance(element.getProject()).createSmartPsiElementPointer(element);
    }

    // --- DataContext utilities ---

    @Nullable
    public static PsiFile getPsiFile(@NotNull DataContext context) {
        return context.getData(CommonDataKeys.PSI_FILE);
    }

    @Nullable
    public static Editor getEditor(@NotNull DataContext context) {
        return context.getData(CommonDataKeys.EDITOR);
    }

    @Nullable
    public static Project getProject(@NotNull DataContext context) {
        return context.getData(CommonDataKeys.PROJECT);
    }

    @Nullable
    public static PsiElement getElementUnderCaretInEditor(@NotNull DataContext context) {
        PsiFile psiFile = getPsiFile(context);
        if (psiFile == null) return null;
        Editor editor = getEditor(context);
        if (editor == null) return null;
        return psiFile.findElementAt(editor.getCaretModel().getOffset());
    }

    // --- Feature flags ---

    public static boolean isFeatureEnabled(@NotNull String featureId) {
        if (isHeadlessEnvironment()) {
            String value = System.getProperty(featureId);
            if (value != null) {
                if ("true".equals(value)) return true;
                if ("false".equals(value)) return false;
            }
        }
        return Experiments.getInstance().isFeatureEnabled(featureId);
    }

    public static void setFeatureEnabled(@NotNull String featureId, boolean enabled) {
        Experiments.getInstance().setFeatureEnabled(featureId, enabled);
    }

    public static <T> T runWithEnabledFeatures(@NotNull String[] featureIds, @NotNull Supplier<T> action) {
        Map<String, Boolean> currentValues = new LinkedHashMap<>();
        for (String featureId : featureIds) {
            currentValues.put(featureId, isFeatureEnabled(featureId));
            setFeatureEnabled(featureId, true);
        }
        try {
            return action.get();
        } finally {
            for (Map.Entry<String, Boolean> entry : currentValues.entrySet()) {
                setFeatureEnabled(entry.getKey(), entry.getValue());
            }
        }
    }

    // --- Cached computation ---

    /**
     * Returns result of provider and stores it in dataHolder along with dependency.
     * If stored dependency equals dependency, then returns stored result without invoking provider.
     */
    public static <T, D> T getCachedOrCompute(@NotNull UserDataHolder dataHolder,
                                               @NotNull Key<SoftReference<com.intellij.openapi.util.Pair<T, D>>> key,
                                               @NotNull D dependency,
                                               @NotNull Supplier<T> provider) {
        SoftReference<com.intellij.openapi.util.Pair<T, D>> ref = dataHolder.getUserData(key);
        if (ref != null) {
            com.intellij.openapi.util.Pair<T, D> oldResult = ref.get();
            if (oldResult != null && Objects.equals(oldResult.second, dependency)) {
                return oldResult.first;
            }
        }
        T value = provider.get();
        dataHolder.putUserData(key, new SoftReference<>(com.intellij.openapi.util.Pair.create(value, dependency)));
        return value;
    }

    // --- Non-blocking read action ---

    /**
     * Intended to be invoked from EDT.
     */
    public static <R> void nonBlocking(@NotNull Project project,
                                        @NotNull Supplier<R> block,
                                        @NotNull Consumer<R> uiContinuation) {
        if (isUnitTestMode()) {
            R result = block.get();
            uiContinuation.accept(result);
        } else {
            ReadAction.nonBlocking((Callable<R>) block::get)
                .inSmartMode(project)
                .expireWith(RsPluginDisposable.getInstance(project))
                .finishOnUiThread(ModalityState.current(), uiContinuation::accept)
                .submit(AppExecutorUtil.getAppExecutorService());
        }
    }

    // --- Settings dialog ---

    public static <T extends Configurable> void showSettingsDialog(@NotNull Project project, @NotNull Class<T> configurableClass) {
        ShowSettingsUtil.getInstance().showSettingsDialog(project, configurableClass);
    }

    // --- Delegation methods for backward compatibility ---

    /**
     * Delegates to {@link Editor#showErrorHint(com.intellij.openapi.editor.Editor, String, short)}.
     */
    public static void showErrorHint(@NotNull com.intellij.openapi.editor.Editor editor,
                                      @NotNull String text,
                                      short position) {
        EditorExt.showErrorHint(editor, text, position);
    }

    /**
     * Delegates to {@link Editor#showErrorHint(com.intellij.openapi.editor.Editor, String)}.
     */
    public static void showErrorHint(@NotNull com.intellij.openapi.editor.Editor editor,
                                      @NotNull String text) {
        EditorExt.showErrorHint(editor, text);
    }

    /**
     * Delegates to {@link UiUtil#selectElement}.
     */
    public static void selectElement(@NotNull org.rust.lang.core.psi.ext.RsElement element,
                                      @NotNull com.intellij.openapi.editor.Editor editor) {
        UiUtil.selectElement(element, editor);
    }

    /**
     * Delegates to {@link UiUtil#pathToRsFileTextField}.
     */
    @NotNull
    public static com.intellij.openapi.ui.TextFieldWithBrowseButton pathToRsFileTextField(
        @NotNull com.intellij.openapi.Disposable disposable,
        @NotNull String title,
        @NotNull Project project,
        @Nullable Runnable onTextChanged
    ) {
        return UiUtil.pathToRsFileTextField(disposable, title, project, onTextChanged);
    }

    /**
     * Delegates to {@link UiUtil#pathToRsFileTextField}.
     */
    @NotNull
    public static com.intellij.openapi.ui.TextFieldWithBrowseButton pathToRsFileTextField(
        @NotNull com.intellij.openapi.Disposable disposable,
        @NotNull String title,
        @NotNull Project project
    ) {
        return UiUtil.pathToRsFileTextField(disposable, title, project);
    }
}
