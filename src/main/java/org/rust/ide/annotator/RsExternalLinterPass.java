/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.ide.annotator;

import org.rust.stdext.Lazy;
import com.intellij.codeHighlighting.DirtyScopeTrackingHighlightingPassFactory;
import com.intellij.codeHighlighting.TextEditorHighlightingPass;
import com.intellij.codeHighlighting.TextEditorHighlightingPassRegistrar;
import com.intellij.codeInsight.daemon.impl.DaemonCodeAnalyzerEx;
import com.intellij.codeInsight.daemon.impl.FileStatusMap;
import com.intellij.codeInsight.daemon.impl.HighlightInfo;
import com.intellij.codeInsight.daemon.impl.UpdateHighlightersUtil;
import com.intellij.openapi.Disposable;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.application.ModalityState;

import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.module.ModuleUtil;
import com.intellij.openapi.progress.ProcessCanceledException;
import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.progress.ProgressManager;
import com.intellij.openapi.progress.util.BackgroundTaskUtil;
import com.intellij.openapi.project.DumbAware;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.Disposer;
import com.intellij.psi.PsiFile;
import com.intellij.util.ui.update.MergingUpdateQueue;
import com.intellij.util.ui.update.Update;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
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

    @NotNull
    private final RsExternalLinterPassFactory myFactory;
    @NotNull
    private final PsiFile myFile;
    @NotNull
    private final Editor myEditor;
    @NotNull
    private final List<HighlightInfo> myHighlights = new ArrayList<>();
    @Nullable
    private volatile Lazy<RsExternalLinterResult> myAnnotationInfo;
    @NotNull
    private volatile Disposable myDisposable;

    public RsExternalLinterPass(@NotNull RsExternalLinterPassFactory factory, @NotNull PsiFile file, @NotNull Editor editor) {
        super(file.getProject(), editor.getDocument());
        this.myFactory = factory;
        this.myFile = file;
        this.myEditor = editor;
        this.myDisposable = myProject;
    }

    @Override
    public void doCollectInformation(@NotNull ProgressIndicator progress) {
        myHighlights.clear();
        if (!(myFile instanceof RsFile) || !isAnnotationPassEnabled()) return;

        org.rust.cargo.project.workspace.CargoWorkspace.Target cargoTarget = RsElementExtUtil.getContainingCargoTarget(myFile);
        if (cargoTarget == null) return;

        Disposable moduleOrProject = ModuleUtil.findModuleForFile(myFile);
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
                BackgroundTaskUtil.runUnderDisposeAwareIndicator(myDisposable, () -> {
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
            public boolean canEat(@NotNull Update update) {
                return false;
            }
        };

        if (OpenApiUtil.isUnitTestMode()) {
            update.run();
        } else {
            myFactory.scheduleExternalActivity(update);
        }
    }

    private void doApply(@NotNull RsExternalLinterResult annotationResult) {
        if (!(myFile instanceof RsFile) || !myFile.isValid()) return;
        try {
            RsExternalLinterUtils.addHighlightsForFile(myHighlights, (RsFile) myFile, annotationResult, RustcMessage.Applicability.UNSPECIFIED);
        } catch (Throwable t) {
            if (t instanceof ProcessCanceledException) throw (ProcessCanceledException) t;
            LOG.error(t);
        }
    }

    private void doFinish(@NotNull List<HighlightInfo> highlights) {
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
            DaemonCodeAnalyzerEx.getInstanceEx(myProject).getFileStatusMap().markFileUpToDate(getDocument(), getId());
        }, ModalityState.stateForComponent(myEditor.getComponent()));
    }

    private boolean isAnnotationPassEnabled() {
        return RsProjectSettingsServiceUtil.getExternalLinterSettings(myProject).getRunOnTheFly();
    }
}
