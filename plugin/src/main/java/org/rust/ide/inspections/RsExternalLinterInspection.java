/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.ide.inspections;
import org.rust.cargo.toolchain.RsToolchainLocator;
import consulo.language.editor.inspection.scheme.InspectionToolWrapper;
import consulo.language.editor.inspection.scheme.InspectionProfile;
import consulo.language.editor.inspection.GlobalSimpleInspectionTool;
import consulo.language.editor.inspection.CommonProblemDescriptor;
import consulo.language.editor.inspection.ProblemDescriptionsProcessor;
import consulo.language.editor.inspection.GlobalInspectionContext;

import org.rust.stdext.Lazy;
import org.rust.lang.core.psi.ext.impl.RsElementUtil;
import consulo.language.editor.rawHighlight.HighlightInfo;
import consulo.language.editor.inspection.LocalQuickFix;
import consulo.language.editor.inspection.LocalQuickFixOnPsiElement;
import consulo.language.editor.inspection.LocalQuickFixAndIntentionActionOnPsiElement;
import consulo.language.editor.inspection.ProblemHighlightType;
import consulo.language.editor.inspection.ProblemDescriptor;
import consulo.language.editor.inspection.ProblemsHolder;
import consulo.language.editor.inspection.scheme.InspectionManager;
import consulo.language.editor.inspection.LocalInspectionTool;
import consulo.language.editor.inspection.LocalInspectionToolSession;
import consulo.ide.impl.idea.codeInspection.ex.GlobalInspectionContextImpl;
import consulo.language.editor.inspection.GlobalInspectionContextUtil;
import consulo.language.editor.inspection.reference.RefElement;
import consulo.ide.impl.idea.codeInspection.ui.InspectionToolPresentation;
import consulo.disposer.Disposable;
import consulo.application.ApplicationManager;
import consulo.application.ReadAction;
import consulo.application.progress.ProgressManager;
import consulo.disposer.Disposer;
import consulo.util.dataholder.Key;
import consulo.language.editor.inspection.scheme.InspectionProjectProfileManager;
import consulo.language.psi.PsiElement;
import consulo.language.psi.PsiFile;
import consulo.util.collection.ContainerUtil;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import org.rust.RsBundle;
import org.rust.cargo.api.model.CargoProject;
import org.rust.cargo.api.model.CargoProjectsUtil;
import org.rust.cargo.api.settings.RsProjectSettingsServiceUtil;
import org.rust.cargo.api.workspace.PackageOrigin;
import org.rust.cargo.api.toolchain.RustcMessage;
import org.rust.cargo.toolchain.tools.CargoCheckArgs;
import org.rust.ide.annotator.RsExternalLinterResult;
import org.rust.ide.annotator.RsExternalLinterUtils;
import org.rust.lang.core.crate.Crate;
import org.rust.lang.core.psi.impl.RsFile;
import org.rust.lang.core.psi.ext.RsElement;

import java.util.*;
import java.util.concurrent.Future;
import java.util.stream.Collectors;
import consulo.language.editor.rawHighlight.HighlightDisplayLevel;
import consulo.localize.LocalizeValue;
import consulo.project.Project;
import org.rust.cargo.runconfig.command.CargoCommandConfiguration;
import org.rust.cargo.toolchain.RsToolchainBase;

public class RsExternalLinterInspection extends GlobalSimpleInspectionTool {

    public static final String SHORT_NAME = "RsExternalLinter";

    private static final Key<Set<RsFile>> ANALYZED_FILES = Key.create("ANALYZED_FILES");

    @Override
    public void inspectionStarted(
        @Nonnull InspectionManager manager,
        @Nonnull GlobalInspectionContext globalContext,
        @Nonnull ProblemDescriptionsProcessor problemDescriptionsProcessor,
        @Nonnull Object state
    ) {
        globalContext.putUserData(ANALYZED_FILES, ContainerUtil.newConcurrentSet());
    }

    @Override
    public void checkFile(
        @Nonnull PsiFile file,
        @Nonnull InspectionManager manager,
        @Nonnull ProblemsHolder problemsHolder,
        @Nonnull GlobalInspectionContext globalContext,
        @Nonnull ProblemDescriptionsProcessor problemDescriptionsProcessor,
        @Nonnull Object state
    ) {
        if (!(file instanceof RsFile)) return;
        RsFile rsFile = (RsFile) file;
        if (Crate.asNotFake(rsFile.getContainingCrate()) == null) return;
        if (Crate.asNotFake(rsFile.getContainingCrate()).getOrigin() != PackageOrigin.WORKSPACE) return;
        Set<RsFile> analyzedFiles = globalContext.getUserData(ANALYZED_FILES);
        if (analyzedFiles == null) return;
        analyzedFiles.add(rsFile);
    }

    @Override
    public void inspectionFinished(
        @Nonnull InspectionManager manager,
        @Nonnull GlobalInspectionContext globalContext,
        @Nonnull ProblemDescriptionsProcessor problemDescriptionsProcessor,
        @Nonnull Object state
    ) {
        if (!(globalContext instanceof GlobalInspectionContextImpl)) return;
        GlobalInspectionContextImpl contextImpl = (GlobalInspectionContextImpl) globalContext;
        Set<RsFile> analyzedFiles = globalContext.getUserData(ANALYZED_FILES);
        if (analyzedFiles == null) return;

        consulo.project.Project project = manager.getProject();
        InspectionProfile currentProfile =
            InspectionProjectProfileManager.getInstance(project).getCurrentProfile();
        consulo.language.editor.inspection.scheme.InspectionToolWrapper<?> toolWrapper =
            currentProfile.getInspectionTool(SHORT_NAME, project);
        if (toolWrapper == null) return;

        while (true) {
            Disposable disposable = RsExternalLinterUtils.createDisposableOnAnyPsiChange(project.getMessageBus());
            Disposer.register(project, disposable);
            Collection<CargoProject> allProjects = CargoProjectsUtil.getCargoProjects(project).getAllProjects();
            Set<CargoProject> cargoProjects;
            if (allProjects.size() == 1) {
                cargoProjects = Collections.singleton(allProjects.iterator().next());
            } else {
                cargoProjects = analyzedFiles.stream()
                    .map(RsFile::getCargoProject)
                    .filter(Objects::nonNull)
                    .collect(Collectors.toSet());
            }
            List<Future<RsExternalLinterResult>> futures = new ArrayList<>();
            for (CargoProject cp : cargoProjects) {
                futures.add(ApplicationManager.getApplication().executeOnPooledThread(() -> {
                    Lazy<RsExternalLinterResult> lazy = checkProjectLazily(cp, disposable);
                    return lazy != null ? lazy.getValue() : null;
                }));
            }
            List<RsExternalLinterResult> annotationResults = new ArrayList<>();
            for (Future<RsExternalLinterResult> future : futures) {
                try {
                    RsExternalLinterResult result = future.get();
                    if (result != null) {
                        annotationResults.add(result);
                    }
                } catch (Exception e) {
                    // ignore
                }
            }

            boolean exit = ReadAction.compute(() -> {
                ProgressManager.checkCanceled();
                if (Disposer.isDisposed(disposable)) return false;
                if (annotationResults.size() < cargoProjects.size()) return true;
                for (RsExternalLinterResult annotationResult : annotationResults) {
                    List<ProblemDescriptor> problemDescriptors = getProblemDescriptors(analyzedFiles, annotationResult);
                    InspectionToolPresentation presentation = contextImpl.getPresentation(toolWrapper);
                    addProblemDescriptors(presentation, problemDescriptors, globalContext);
                }
                return true;
            });

            if (exit) break;
        }
    }

    @Nonnull
    @Override
    public consulo.language.editor.rawHighlight.HighlightDisplayLevel getDefaultLevel() {
        return consulo.language.editor.rawHighlight.HighlightDisplayLevel.WARNING;
    }

    @Nonnull
    @Override
    public String getShortName() {
        return SHORT_NAME;
    }

    private static Lazy<RsExternalLinterResult> checkProjectLazily(
        @Nonnull CargoProject cargoProject,
        @Nonnull Disposable disposable
    ) {
        return ReadAction.compute(() -> {
            consulo.project.Project project = cargoProject.getProject();
            RsToolchainBase toolchain = RsToolchainLocator.getToolchain(project);
            if (toolchain == null) return null;
            return RsExternalLinterUtils.checkLazily(
                toolchain,
                project,
                disposable,
                org.rust.cargo.project.model.CargoProjectLocator.getWorkingDirectory(cargoProject),
                CargoCheckArgs.forCargoProject(cargoProject)
            );
        });
    }

    private static List<ProblemDescriptor> getProblemDescriptors(
        @Nonnull Set<RsFile> analyzedFiles,
        @Nonnull RsExternalLinterResult annotationResult
    ) {
        List<ProblemDescriptor> result = new ArrayList<>();
        for (RsFile file : analyzedFiles) {
            if (!file.isValid()) continue;
            List<HighlightInfo> highlights = new ArrayList<>();
            RsExternalLinterUtils.addHighlightsForFile(highlights, file, annotationResult, RustcMessage.Applicability.MACHINE_APPLICABLE);
            for (HighlightInfo highlight : highlights) {
                ProblemDescriptor descriptor = highlightInfoToProblemDescriptor(file, highlight);
                if (descriptor != null) {
                    result.add(descriptor);
                }
            }
        }
        return result;
    }

    private static void addProblemDescriptors(
        @Nonnull InspectionToolPresentation presentation,
        @Nonnull List<ProblemDescriptor> descriptors,
        @Nonnull GlobalInspectionContext context
    ) {
        if (descriptors.isEmpty()) return;
        Map<RefElement, List<ProblemDescriptor>> problems = new HashMap<>();

        for (ProblemDescriptor descriptor : descriptors) {
            PsiElement element = descriptor.getPsiElement();
            if (element == null) continue;
            RefElement refElement = getProblemElement(element, context);
            if (refElement == null) continue;
            List<ProblemDescriptor> elementProblems = problems.computeIfAbsent(refElement, k -> new ArrayList<>());
            elementProblems.add(descriptor);
        }

        for (Map.Entry<RefElement, List<ProblemDescriptor>> entry : problems.entrySet()) {
            CommonProblemDescriptor[] descriptions = entry.getValue().toArray(new CommonProblemDescriptor[0]);
            presentation.addProblemElement(entry.getKey(), false, descriptions);
        }
    }

    @Nullable
    private static ProblemDescriptor highlightInfoToProblemDescriptor(@Nonnull PsiFile file, @Nonnull HighlightInfo highlight) {
        PsiElement startElement = file.findElementAt(highlight.getStartOffset());
        if (startElement == null) return null;
        PsiElement endElement = file.findElementAt(highlight.getEndOffset() - 1);
        if (endElement == null) endElement = startElement;
        InspectionManager manager = InspectionManager.getInstance(file.getProject());
        String description = String.valueOf(highlight.getDescription());
        if (description == null || "null".equals(description)) description = "";
        return manager.createProblemDescriptor(
            startElement,
            endElement,
            description,
            ProblemHighlightType.GENERIC_ERROR_OR_WARNING,
            false
        );
    }

    private static RefElement getProblemElement(@Nonnull PsiElement element, @Nonnull GlobalInspectionContext context) {
        RsFile problemElement = RsElementUtil.ancestorOrSelf(element, RsFile.class);
        RefElement refElement = context.getRefManager().getReference(problemElement);
        if (refElement == null && problemElement != null) {
            return GlobalInspectionContextUtil.retrieveRefElement(element, context);
        } else {
            return refElement;
        }
    }

    @Override
    @jakarta.annotation.Nonnull
    public consulo.localize.LocalizeValue getDisplayName() {
        return consulo.localize.LocalizeValue.of(org.rust.RsBundle.message("inspection.rs.external.linter.display.name"));
    }

    @Override
    @jakarta.annotation.Nonnull
    public consulo.localize.LocalizeValue getGroupDisplayName() {
        return consulo.localize.LocalizeValue.of(org.rust.RsBundle.message("rust"));
    }
}
