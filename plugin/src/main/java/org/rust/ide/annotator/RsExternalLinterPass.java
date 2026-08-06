/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.ide.annotator;
import consulo.language.editor.FileStatusMap;

import org.rust.stdext.Lazy;
import consulo.language.editor.impl.highlight.DirtyScopeTrackingHighlightingPassFactory;
import consulo.language.editor.highlight.TextEditorHighlightingPass;
import com.intellij.codeHighlighting.TextEditorHighlightingPassRegistrar;
import consulo.language.editor.rawHighlight.HighlightInfo;
import consulo.language.editor.highlight.UpdateHighlightersUtil;
import consulo.disposer.Disposable;
import consulo.language.util.ModuleUtilCore;
import consulo.application.ApplicationManager;
import consulo.ui.ModalityState;

import consulo.logging.Logger;
import consulo.codeEditor.Editor;
// duplicate ModuleUtilCore import removed (consulo.language.util version kept)
import consulo.component.ProcessCanceledException;
import consulo.application.progress.ProgressIndicator;
import consulo.application.progress.ProgressManager;
import consulo.application.dumb.DumbAware;
import consulo.project.Project;
import consulo.disposer.Disposer;
import consulo.language.psi.PsiFile;
import consulo.ui.ex.awt.util.MergingUpdateQueue;
import consulo.ui.ex.awt.util.Update;
import consulo.language.editor.DaemonCodeAnalyzer;
import consulo.application.progress.EmptyProgressIndicator;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import org.rust.cargo.project.settings.RsExternalLinterSettingsUtil;
import org.rust.cargo.project.settings.ToolchainExtUtil;
import org.rust.cargo.project.workspace.PackageOrigin;
import org.rust.cargo.toolchain.impl.RustcMessage;
import org.rust.cargo.toolchain.tools.CargoCheckArgs;
import org.rust.ide.notifications.RsExternalLinterSlowRunNotifier;
import org.rust.lang.core.psi.RsFile;
import org.rust.lang.core.psi.ext.RsElementUtil;
import org.rust.openapiext.OpenApiUtil;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import org.rust.cargo.project.settings.RsProjectSettingsServiceUtil;
import org.rust.lang.core.psi.ext.RsElementExtUtil;

public class RsExternalLinterPass extends TextEditorHighlightingPass implements DumbAware {
    private static final Logger LOG = Logger.getInstance(RsExternalLinterPass.class);

    @Nonnull
    private final RsExternalLinterPassFactory myFactory;
    @Nonnull
    private final PsiFile myFile;
    @Nonnull
    private final Editor myEditor;
    @Nonnull
    private final List<HighlightInfo> myHighlights = new ArrayList<>();
    @Nullable
    private volatile Lazy<RsExternalLinterResult> myAnnotationInfo;
    @Nonnull
    private volatile Disposable myDisposable;

    public RsExternalLinterPass(@Nonnull RsExternalLinterPassFactory factory, @Nonnull PsiFile file, @Nonnull Editor editor) {
        super(file.getProject(), editor.getDocument());
        this.myFactory = factory;
        this.myFile = file;
        this.myEditor = editor;
        this.myDisposable = myProject;
    }

    @Override
    public void doCollectInformation(@Nonnull ProgressIndicator progress) {
        myHighlights.clear();
        if (!(myFile instanceof RsFile) || !isAnnotationPassEnabled()) return;

        org.rust.cargo.project.workspace.CargoWorkspace.Target cargoTarget = RsElementExtUtil.getContainingCargoTarget(myFile);
        if (cargoTarget == null) return;

        Disposable moduleOrProject = (Disposable) ModuleUtilCore.findModuleForPsiElement(myFile);
        if (moduleOrProject == null) moduleOrProject = myProject;
        myDisposable = RsExternalLinterUtils.createDisposableOnAnyPsiChange(myProject.getMessageBus());
        Disposer.register(moduleOrProject, myDisposable);

        CargoCheckArgs args = CargoCheckArgs.forTarget(myProject, cargoTarget);
        Object toolchain = RsProjectSettingsServiceUtil.getToolchain(myProject);
        if (toolchain == null) return;

        myAnnotationInfo = RsExternalLinterUtils.checkLazily(
            toolchain,
            myProject,
            myDisposable,
            args
        );
    }

    @Override
    public void doApplyInformationToEditor() {
        if (!(myFile instanceof RsFile)) return;

        if (myAnnotationInfo == null || !isAnnotationPassEnabled()) {
            myDisposable = myProject;
            doFinish(Collections.emptyList());
            return;
        }

        Update update = new Update(myFile) {
            @Override
            public void setRejected() {
                super.setRejected();
                doFinish(myHighlights);
            }

            @Override
            public void run() {
                runUnderDisposeAwareIndicator(myDisposable, () -> {
                    Lazy<RsExternalLinterResult> info = myAnnotationInfo;
                    if (info == null) return;
                    RsExternalLinterResult annotationResult = info.getValue();
                    if (annotationResult == null) return;
                    myProject.getService(RsExternalLinterSlowRunNotifier.class).reportDuration(annotationResult.getExecutionTime());
                    ApplicationManager.getApplication().runReadAction(() -> {
                        ProgressManager.checkCanceled();
                        doApply(annotationResult);
                        ProgressManager.checkCanceled();
                        doFinish(myHighlights);
                    });
                });
            }

            @Override
            public boolean canEat(@Nonnull Update update) {
                return false;
            }
        };

        if (OpenApiUtil.isUnitTestMode()) {
            update.run();
        } else {
            myFactory.scheduleExternalActivity(update);
        }
    }

    private void doApply(@Nonnull RsExternalLinterResult annotationResult) {
        if (!(myFile instanceof RsFile) || !myFile.isValid()) return;
        try {
            RsExternalLinterUtils.addHighlightsForFile(myHighlights, (RsFile) myFile, annotationResult, RustcMessage.Applicability.UNSPECIFIED);
        } catch (Throwable t) {
            if (t instanceof ProcessCanceledException) throw (ProcessCanceledException) t;
            LOG.error(t);
        }
    }

    private void doFinish(@Nonnull List<HighlightInfo> highlights) {
        ApplicationManager.getApplication().invokeLater(() -> {
            if (Disposer.isDisposed(myDisposable)) return;
            UpdateHighlightersUtil.setHighlightersToEditor(
                myProject,
                getDocument(),
                0,
                myFile.getTextLength(),
                highlights,
                getColorsScheme(),
                getId()
            );
            DaemonCodeAnalyzer.getInstance(myProject).getFileStatusMap().markFileUpToDate(getDocument(), getId());
        }, ModalityState.nonModal());
    }

    private boolean isAnnotationPassEnabled() {
        return RsProjectSettingsServiceUtil.getExternalLinterSettings(myProject).getRunOnTheFly();
    }
    /**
     * Runs {@code runnable} under a progress indicator that is cancelled when {@code parent} is
     * disposed. Replaces {@code BackgroundTaskUtil.runUnderDisposeAwareIndicator}, which is
     * platform-internal.
     */
    private static void runUnderDisposeAwareIndicator(@Nonnull Disposable parent, @Nonnull Runnable runnable) {
        EmptyProgressIndicator indicator = new EmptyProgressIndicator();
        Disposable cancelOnDispose = indicator::cancel;
        if (!Disposer.tryRegister(parent, cancelOnDispose)) {
            // parent is already disposed - nothing to run
            return;
        }
        try {
            ProgressManager.getInstance().runProcess(runnable, indicator);
        }
        catch (ProcessCanceledException ignored) {
            // disposal cancelled the work
        }
        finally {
            Disposer.dispose(cancelOnDispose);
        }
    }
}
