/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.openapiext;
import consulo.language.psi.SmartPointerManager;
import consulo.ide.impl.idea.ide.plugins.PluginManagerCore;
import consulo.util.lang.EmptyRunnable;
import consulo.application.util.function.ThrowableComputable;
import consulo.application.util.RecursionManager;
import consulo.language.psi.PsiDocumentManager;
import consulo.util.jdom.JDOMUtil;
import consulo.application.util.function.Computable;
import consulo.util.dataholder.UserDataHolder;

import consulo.container.plugin.PluginDescriptor;
import consulo.container.plugin.PluginManager;
import consulo.disposer.Disposable;
import consulo.language.editor.CommonDataKeys;
import consulo.dataContext.DataContext;
import consulo.application.Application;
import consulo.application.ApplicationManager;
import consulo.application.ReadAction;
import consulo.application.WriteAction;
import consulo.ui.ModalityState;
import consulo.application.util.ApplicationUtil;
import consulo.undoRedo.CommandProcessor;
import consulo.language.editor.WriteCommandAction;
import consulo.document.Document;
import consulo.codeEditor.Editor;
import consulo.container.plugin.PluginId;
import consulo.document.FileDocumentManager;
import consulo.module.Module;
import consulo.module.ModuleManager;
import consulo.configurable.Configurable;
import consulo.ide.setting.ShowSettingsUtil;
import consulo.application.progress.EmptyProgressIndicator;
import consulo.component.ProcessCanceledException;
import consulo.application.progress.ProgressIndicator;
import consulo.application.progress.ProgressManager;
import consulo.application.progress.ProgressIndicatorListener;
import consulo.application.event.ApplicationListener;
import consulo.disposer.Disposer;
import consulo.project.DumbService;
import consulo.application.dumb.IndexNotReadyException;
import consulo.project.Project;
import consulo.util.dataholder.Key;
import consulo.util.lang.Pair;
import consulo.document.util.TextRange;
import consulo.util.dataholder.UserDataHolderBase;
import consulo.util.lang.StringUtil;
import consulo.util.lang.ref.Ref;
import consulo.util.lang.ThreeState;
import consulo.component.util.ModificationTracker;
import consulo.util.io.FileUtil;
import consulo.util.lang.StringUtil;
import consulo.virtualFileSystem.util.VirtualFileUtil;
import consulo.virtualFileSystem.VirtualFile;
import consulo.virtualFileSystem.VirtualFileWithId;
import consulo.language.psi.PsiElement;
import consulo.language.psi.PsiFile;
import consulo.language.psi.PsiReference;
import consulo.language.psi.PsiManager;
import consulo.language.psi.PsiWhiteSpace;
import consulo.language.psi.PsiComment;
import consulo.language.psi.PsiNamedElement;
import consulo.language.psi.PsiNameIdentifierOwner;
import consulo.language.psi.PsiDirectory;
import consulo.language.psi.PsiErrorElement;
import consulo.language.psi.PsiElementVisitor;
import consulo.language.psi.PsiElementResolveResult;
import consulo.language.psi.ResolveResult;
import consulo.language.psi.PsiPolyVariantReference;
import consulo.language.psi.PsiReferenceBase;
import consulo.language.psi.PsiUtilCore;
import consulo.language.psi.StubBasedPsiElement;
import consulo.language.psi.SmartPsiElementPointer;
import consulo.language.psi.ElementManipulators;
import consulo.language.psi.ElementManipulator;
import consulo.language.psi.LiteralTextEscaper;
import consulo.language.psi.NavigatablePsiElement;
import consulo.language.psi.PsiLanguageInjectionHost;
import consulo.language.psi.ContributedReferenceHost;
import consulo.language.psi.SyntaxTraverser;
import consulo.language.psi.scope.GlobalSearchScope;
import consulo.language.psi.stub.StubIndex;
import consulo.language.psi.stub.StubIndexKey;
import consulo.application.util.concurrent.AppExecutorUtil;
import consulo.ui.ex.awt.UIUtil;
import org.jdom.Element;
import org.jdom.input.SAXBuilder;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import org.rust.cargo.RustfmtWatcher;
import org.rust.ide.annotator.RsExternalLinterPass;

import java.io.ByteArrayInputStream;
import java.lang.ref.SoftReference;
import org.rust.ide.experiments.RsExperiments;

import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.concurrent.Callable;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;
import consulo.ui.ex.awt.TextFieldWithBrowseButton;
import org.rust.lang.core.psi.ext.RsElement;

/**
 * Utility methods for working with the platform API.
 */
public final class OpenApiUtil {
    private OpenApiUtil() {
    }

    @Nonnull
    public static final String PLUGIN_ID = "consulo.rust";

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

    public static boolean isUnderDarkTheme() { return consulo.ui.ex.awt.UIUtil.isUnderDarkTheme(); }

    // --- Write command actions ---

    /**
     * Perform a write action for the provided project.
     *
     * @param project     the project
     * @param commandName the name of the action which will appear in the Undo/Redo stack
     * @param files       files modified by the action
     * @param command     the write action to perform
     */
    public static <T> T runWriteCommandAction(@Nonnull Project project,
                                               @Nonnull String commandName,
                                               @Nonnull PsiFile[] files,
                                               @Nonnull Supplier<T> command) {
        return WriteCommandAction.writeCommandAction(project)
            .withName(commandName)
            .compute(command::get);
    }

    /**
     * Overload without files parameter.
     */
    public static <T> T runWriteCommandAction(@Nonnull Project project,
                                               @Nonnull String commandName,
                                               @Nonnull Supplier<T> command) {
        return runWriteCommandAction(project, commandName, new PsiFile[0], command);
    }

    /**
     * Overload accepting a Runnable (void action) without files parameter.
     */
    public static void runWriteCommandAction(@Nonnull Project project,
                                              @Nonnull String commandName,
                                              @Nonnull Runnable command) {
        runWriteCommandAction(project, commandName, new PsiFile[0], () -> { command.run(); return null; });
    }

    /**
     * Modification of runUndoTransparentWriteAction which applies formatting to modified code.
     */
    public static void runUndoTransparentWriteCommandAction(@Nonnull Project project, @Nonnull Runnable command) {
        CommandProcessor.getInstance().runUndoTransparentAction(() ->
            WriteCommandAction.runWriteCommandAction(project, command)
        );
    }

    // --- Project utilities ---

    @Nonnull
    public static Collection<Module> getModules(@Nonnull Project project) {
        return Arrays.asList(ModuleManager.getInstance(project).getModules());
    }

    // --- Recursion guard ---

    @Nullable
    public static <T> T recursionGuard(@Nonnull Object key, @Nonnull Computable<T> block, boolean memoize) {
        return RecursionManager.doPreventingRecursion(key, memoize, block);
    }

    @Nullable
    public static <T> T recursionGuard(@Nonnull Object key, @Nonnull Computable<T> block) {
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

    public static void checkIsSmartMode(@Nonnull Project project) {
        if (DumbService.getInstance(project).isDumb()) throw IndexNotReadyException.create();
    }

    public static void checkCommitIsNotInProgress(@Nonnull Project project) {
        Application app = ApplicationManager.getApplication();
        if ((app.isUnitTestMode() || app.isInternal()) && app.isDispatchThread()) {
            // Not checked: there is no public way to query whether a PSI commit is in progress.
            // Accessing indices during PSI event processing hurts typing performance.
        }
    }

    // --- VirtualFile utilities ---

    public static void fullyRefreshDirectory(@Nonnull VirtualFile directory) {
        VirtualFileUtil.markDirtyAndRefresh(false, true, true, directory);
    }

    @Nullable
    public static VirtualFile findFileByMaybeRelativePath(@Nonnull VirtualFile base, @Nonnull String path) {
        if (FileUtil.isAbsolute(path)) {
            return base.getFileSystem().findFileByPath(path);
        } else {
            return base.findFileByRelativePath(path);
        }
    }

    @Nonnull
    public static Pair<VirtualFile, List<String>> findNearestExistingFile(@Nonnull VirtualFile base, @Nonnull String path) {
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

    @Nonnull
    public static Path getPathAsPath(@Nonnull VirtualFile file) {
        return Paths.get(file.getPath());
    }

    @Nullable
    public static PsiFile toPsiFile(@Nonnull VirtualFile file, @Nonnull Project project) {
        return PsiManager.getInstance(project).findFile(file);
    }

    @Nullable
    public static PsiDirectory toPsiDirectory(@Nonnull VirtualFile file, @Nonnull Project project) {
        return PsiManager.getInstance(project).findDirectory(file);
    }

    @Nullable
    public static PsiFile toPsiFile(@Nonnull Document document, @Nonnull Project project) {
        return PsiDocumentManager.getInstance(project).getPsiFile(document);
    }

    @Nullable
    public static VirtualFile getVirtualFile(@Nonnull Document document) {
        return FileDocumentManager.getInstance().getFile(document);
    }

    @Nullable
    public static Document getDocument(@Nonnull VirtualFile file) {
        return FileDocumentManager.getInstance().getDocument(file);
    }

    @Nullable
    public static Document getDocument(@Nonnull PsiFile file) {
        return file.getViewProvider().getDocument();
    }

    public static int getFileId(@Nonnull VirtualFile file) {
        return ((VirtualFileWithId) file).getId();
    }

    // --- StubIndex utilities ---

    @Nonnull
    public static <Key, Psi extends PsiElement> Collection<Psi> getElements(
        @Nonnull StubIndexKey<Key, Psi> indexKey,
        @Nonnull Key key,
        @Nonnull Project project,
        @Nullable GlobalSearchScope scope,
        @Nonnull Class<Psi> requiredClass
    ) {
        return StubIndex.getElements(indexKey, key, project, scope, requiredClass);
    }

    /**
     * Callers should prefer the 5-parameter overload with an explicit class for type safety.
     */
    @SuppressWarnings("unchecked")
    @Nonnull
    public static <Key, Psi extends PsiElement> Collection<Psi> getElements(
        @Nonnull StubIndexKey<Key, Psi> indexKey,
        @Nonnull Key key,
        @Nonnull Project project,
        @Nullable GlobalSearchScope scope
    ) {
        return StubIndex.getElements(indexKey, key, project, scope, (Class<Psi>) PsiElement.class);
    }

    // --- XML utilities ---

    @Nonnull
    public static String toXmlString(@Nonnull Element element) {
        return JDOMUtil.writeElement(element);
    }

    @Nonnull
    public static Element elementFromXmlString(@Nonnull String xml) {
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
     * Saves all documents "as they are" (without trailing spaces stripping).
     * <p>
     * Upstream additionally queued each document for later stripping by reflecting into
     * {@code FileDocumentManagerImpl.myTrailingSpacesStripper}; both classes are platform-internal
     * and the field poking would not survive JPMS anyway, so that part is dropped.
     */
    public static void saveAllDocumentsAsTheyAre(boolean reformatLater) {
        FileDocumentManager documentManager = FileDocumentManager.getInstance();
        RustfmtWatcher rustfmtWatcher = RustfmtWatcher.getInstance();
        rustfmtWatcher.withoutReformatting(() -> {
            for (Document document : documentManager.getUnsavedDocuments()) {
                documentManager.saveDocumentAsIs(document);
                if (reformatLater) rustfmtWatcher.reformatDocumentLater(document);
            }
        });
    }

    public static void saveAllDocumentsAsTheyAre() {
        saveAllDocumentsAsTheyAre(true);
    }

    // --- Test assertions ---

    public static void testAssert(@Nonnull Supplier<Boolean> action) {
        testAssert(action, () -> "Assertion failed");
    }

    public static void testAssert(@Nonnull Supplier<Boolean> action, @Nonnull Supplier<Object> lazyMessage) {
        if (isUnitTestMode() && !action.get()) {
            Object message = lazyMessage.get();
            throw new AssertionError(message);
        }
    }

    // --- Progress utilities ---

    @Nonnull
    public static <T> T runWithCheckCanceled(@Nonnull Supplier<T> callable) {
        try {
            return ApplicationUtil.runWithCheckCanceled(
                (Callable<T>) callable::get,
                ProgressManager.getInstance().getProgressIndicator()
            );
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public static <T> T computeWithCancelableProgress(@Nonnull Project project,
                                                       @Nonnull String title,
                                                       @Nonnull Supplier<T> supplier) {
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

    public static boolean runWithCancelableProgress(@Nonnull Project project,
                                                     @Nonnull String title,
                                                     @Nonnull Runnable process) {
        if (isUnitTestMode()) {
            process.run();
            return true;
        }
        return ProgressManager.getInstance().runProcessWithProgressSynchronously(process, title, true, project);
    }

    // --- UserDataHolder utilities ---

    @Nonnull
    public static <T> T getOrPut(@Nonnull UserDataHolder holder, @Nonnull Key<T> key, @Nonnull Supplier<T> defaultValue) {
        T data = holder.getUserData(key);
        if (data != null) return data;
        T value = defaultValue.get();
        holder.putUserData(key, value);
        return value;
    }

    // --- Plugin utilities ---

    @Nonnull
    public static PluginDescriptor plugin() {
        PluginDescriptor descriptor = consulo.container.plugin.PluginManager.findPlugin(PluginId.getId(PLUGIN_ID));
        assert descriptor != null : "Plugin descriptor not found for " + PLUGIN_ID;
        return descriptor;
    }

    // --- String utilities ---

    @Nonnull
    public static String getEscaped(@Nonnull String text) {
        return StringUtil.escapeXmlEntities(text);
    }

    @Nonnull
    public static String escaped(@Nonnull String text) {
        return StringUtil.escapeXmlEntities(text);
    }

    // --- Smart mode utilities ---

    public static <T> T runReadActionInSmartMode(@Nonnull DumbService dumbService, @Nonnull Supplier<T> action) {
        ProgressManager.checkCanceled();
        if (dumbService.getProject().isDisposed()) throw new ProcessCanceledException();
        return dumbService.runReadActionInSmartMode((Computable<T>) () -> {
            ProgressManager.checkCanceled();
            return action.get();
        });
    }

    // --- Write action priority utilities ---

    @Nonnull
    public static <T> T executeUnderProgressWithWriteActionPriorityWithRetries(
        @Nonnull ProgressIndicator indicator,
        @Nonnull Function<ProgressIndicator, T> action
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
            RsSensitiveProgressWrapper wrappedIndicator = new RsSensitiveProgressWrapper(indicator);
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

    /**
     * Runs {@code action} under {@code indicator}, cancelling the indicator as soon as any thread
     * wants the write lock.
     * <p>
     * {@code ProgressIndicatorUtils.runWithWriteActionPriority} is platform-internal and has no
     * public counterpart (unlike the read-action variant below), so the write-action listener is
     * wired up by hand here.
     *
     * @return {@code true} if the action ran to completion, {@code false} if it was cancelled
     */
    public static boolean runWithWriteActionPriority(@Nonnull ProgressIndicator indicator, @Nonnull Runnable action) {
        Disposable listenerDisposable = Disposable.newDisposable("RsWriteActionPriority");
        try {
            ApplicationManager.getApplication().addApplicationListener(new ApplicationListener() {
                @Override
                public void beforeWriteActionStart(Object actionClass) {
                    indicator.cancel();
                }
            }, listenerDisposable);
            if (indicator.isCanceled()) return false;
            ProgressManager.getInstance().runProcess(action, indicator);
        }
        catch (ProcessCanceledException e) {
            return false;
        }
        finally {
            Disposer.dispose(listenerDisposable);
        }
        return !indicator.isCanceled();
    }

    public static boolean runInReadActionWithWriteActionPriority(@Nonnull ProgressIndicator indicator, @Nonnull Runnable action) {
        return ProgressManager.getInstance().runInReadActionWithWriteActionPriority(action, indicator);
    }

    @Nonnull
    public static <T> T computeInReadActionWithWriteActionPriority(@Nonnull ProgressIndicator indicator,
                                                                    @Nonnull Supplier<T> action) {
        @SuppressWarnings("unchecked")
        T[] result = (T[]) new Object[1];
        boolean success = runInReadActionWithWriteActionPriority(indicator, () -> {
            result[0] = action.get();
        });
        if (!success) throw new ProcessCanceledException();
        return result[0];
    }

    public static <T> T executeUnderProgress(@Nonnull ProgressIndicator indicator, @Nonnull Supplier<T> action) {
        @SuppressWarnings("unchecked")
        T[] result = (T[]) new Object[1];
        ProgressManager.getInstance().executeProcessUnderProgress(() -> result[0] = action.get(), indicator);
        return result[0];
    }

    // --- Thread-safe progress indicator ---

    @Nonnull
    public static ProgressIndicator toThreadSafeProgressIndicator(@Nonnull ProgressIndicator indicator) {
        // Upstream attached an AbstractProgressIndicatorExBase state delegate via ProgressIndicatorEx;
        // both are platform-internal. The public ProgressIndicatorListener carries the one signal
        // that actually mattered here — cancellation.
        EmptyProgressIndicator threadSafeIndicator = new EmptyProgressIndicator();
        indicator.addListener(new ProgressIndicatorListener() {
            @Override
            public void canceled() {
                threadSafeIndicator.cancel();
            }
        });
        return threadSafeIndicator;
    }

    // --- Smart pointer utilities ---

    @Nonnull
    public static <T extends PsiElement> SmartPsiElementPointer<T> createSmartPointer(@Nonnull T element) {
        return SmartPointerManager.getInstance(element.getProject()).createSmartPsiElementPointer(element);
    }

    // --- DataContext utilities ---

    @Nullable
    public static PsiFile getPsiFile(@Nonnull DataContext context) {
        return context.getData(CommonDataKeys.PSI_FILE);
    }

    @Nullable
    public static Editor getEditor(@Nonnull DataContext context) {
        return context.getData(CommonDataKeys.EDITOR);
    }

    @Nullable
    public static Project getProject(@Nonnull DataContext context) {
        return context.getData(CommonDataKeys.PROJECT);
    }

    @Nullable
    public static PsiElement getElementUnderCaretInEditor(@Nonnull DataContext context) {
        PsiFile psiFile = getPsiFile(context);
        if (psiFile == null) return null;
        Editor editor = getEditor(context);
        if (editor == null) return null;
        return psiFile.findElementAt(editor.getCaretModel().getOffset());
    }

    // --- Feature flags ---

    /**
     * Feature ids that are on unless something turns them off. Everything not listed here is off.
     */
    private static final Set<String> ENABLED_BY_DEFAULT_FEATURES = Set.of(
        RsExperiments.BUILD_TOOL_WINDOW,
        RsExperiments.EVALUATE_BUILD_SCRIPTS,
        RsExperiments.FETCH_ACTUAL_STDLIB_METADATA,
        RsExperiments.FN_LIKE_PROC_MACROS,
        RsExperiments.DERIVE_PROC_MACROS,
        RsExperiments.ATTR_PROC_MACROS,
        RsExperiments.CRATES_LOCAL_INDEX,
        RsExperiments.WSL_TOOLCHAIN
    );

    /** Per-session overrides set through {@link #setFeatureEnabled}. */
    private static final ConcurrentMap<String, Boolean> FEATURE_OVERRIDES = new ConcurrentHashMap<>();

    /**
     * A feature is on if it was explicitly switched on for this session, else if a system property of the
     * same name says so, else if it is on by default.
     */
    public static boolean isFeatureEnabled(@Nonnull String featureId) {
        Boolean override = FEATURE_OVERRIDES.get(featureId);
        if (override != null) return override;

        String value = System.getProperty(featureId);
        if ("true".equals(value)) return true;
        if ("false".equals(value)) return false;

        return ENABLED_BY_DEFAULT_FEATURES.contains(featureId);
    }

    public static void setFeatureEnabled(@Nonnull String featureId, boolean enabled) {
        FEATURE_OVERRIDES.put(featureId, enabled);
    }

    public static <T> T runWithEnabledFeatures(@Nonnull String[] featureIds, @Nonnull Supplier<T> action) {
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
    public static <T, D> T getCachedOrCompute(@Nonnull UserDataHolder dataHolder,
                                               @Nonnull Key<SoftReference<consulo.util.lang.Pair<T, D>>> key,
                                               @Nonnull D dependency,
                                               @Nonnull Supplier<T> provider) {
        SoftReference<consulo.util.lang.Pair<T, D>> ref = dataHolder.getUserData(key);
        if (ref != null) {
            consulo.util.lang.Pair<T, D> oldResult = ref.get();
            if (oldResult != null && Objects.equals(oldResult.second, dependency)) {
                return oldResult.first;
            }
        }
        T value = provider.get();
        dataHolder.putUserData(key, new SoftReference<>(consulo.util.lang.Pair.create(value, dependency)));
        return value;
    }

    // --- Non-blocking read action ---

    /**
     * Intended to be invoked from EDT.
     */
    public static <R> void nonBlocking(@Nonnull Project project,
                                        @Nonnull Supplier<R> block,
                                        @Nonnull Consumer<R> uiContinuation) {
        if (isUnitTestMode()) {
            R result = block.get();
            uiContinuation.accept(result);
        } else {
            ReadAction.nonBlocking((Callable<R>) block::get)
                .inSmartMode(project)
                .expireWith(RsPluginDisposable.getInstance(project))
                .finishOnUiThread(app -> ModalityState.any(), uiContinuation::accept)
                .submit(AppExecutorUtil.getAppExecutorService());
        }
    }

    // --- Settings dialog ---

    public static <T extends Configurable> void showSettingsDialog(@Nonnull Project project, @Nonnull Class<T> configurableClass) {
        ShowSettingsUtil.getInstance().showSettingsDialog(project, configurableClass);
    }

    // --- Delegation methods for backward compatibility ---

    /**
     * Delegates to {@link Editor#showErrorHint(consulo.codeEditor.Editor, String, short)}.
     */
    public static void showErrorHint(@Nonnull consulo.codeEditor.Editor editor,
                                      @Nonnull String text,
                                      short position) {
        EditorExt.showErrorHint(editor, text, position);
    }

    /**
     * Delegates to {@link Editor#showErrorHint(consulo.codeEditor.Editor, String)}.
     */
    public static void showErrorHint(@Nonnull consulo.codeEditor.Editor editor,
                                      @Nonnull String text) {
        EditorExt.showErrorHint(editor, text);
    }

    /**
     * Delegates to {@link UiUtil#selectElement}.
     */
    public static void selectElement(@Nonnull org.rust.lang.core.psi.ext.RsElement element,
                                      @Nonnull consulo.codeEditor.Editor editor) {
        UiUtil.selectElement(element, editor);
    }

    /**
     * Delegates to {@link UiUtil#pathToRsFileTextField}.
     */
    @Nonnull
    public static consulo.ui.ex.awt.TextFieldWithBrowseButton pathToRsFileTextField(
        @Nonnull consulo.disposer.Disposable disposable,
        @Nonnull String title,
        @Nonnull Project project,
        @Nullable Runnable onTextChanged
    ) {
        return UiUtil.pathToRsFileTextField(disposable, title, project, onTextChanged);
    }

    /**
     * Delegates to {@link UiUtil#pathToRsFileTextField}.
     */
    @Nonnull
    public static consulo.ui.ex.awt.TextFieldWithBrowseButton pathToRsFileTextField(
        @Nonnull consulo.disposer.Disposable disposable,
        @Nonnull String title,
        @Nonnull Project project
    ) {
        return UiUtil.pathToRsFileTextField(disposable, title, project);
    }
}
