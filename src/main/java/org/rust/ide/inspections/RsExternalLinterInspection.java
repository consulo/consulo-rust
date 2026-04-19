/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.ide.inspections;

import org.rust.stdext.Lazy;
import org.rust.lang.core.psi.ext.RsElementUtil;
import com.intellij.codeInsight.daemon.impl.HighlightInfo;
import com.intellij.codeInspection.*;
import com.intellij.codeInspection.ex.GlobalInspectionContextImpl;
import com.intellij.codeInspection.ex.GlobalInspectionContextUtil;
import com.intellij.codeInspection.reference.RefElement;
import com.intellij.codeInspection.ui.InspectionToolPresentation;
import com.intellij.openapi.Disposable;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.application.ReadAction;
import com.intellij.openapi.progress.ProgressManager;
import com.intellij.openapi.util.Disposer;
import com.intellij.openapi.util.Key;
import com.intellij.profile.codeInspection.InspectionProjectProfileManager;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.intellij.util.containers.ContainerUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.rust.RsBundle;
import org.rust.cargo.project.model.CargoProject;
import org.rust.cargo.project.model.CargoProjectsUtil;
import org.rust.cargo.project.settings.RsProjectSettingsServiceUtil;
import org.rust.cargo.project.workspace.PackageOrigin;
import org.rust.cargo.toolchain.impl.RustcMessage;
import org.rust.cargo.toolchain.tools.CargoCheckArgs;
import org.rust.ide.annotator.RsExternalLinterResult;
import org.rust.ide.annotator.RsExternalLinterUtils;
import org.rust.lang.core.crate.Crate;
import org.rust.lang.core.psi.RsFile;
import org.rust.lang.core.psi.ext.RsElement;

import java.util.*;
import java.util.concurrent.Future;
import java.util.stream.Collectors;

public class RsExternalLinterInspection extends GlobalSimpleInspectionTool {

    public static final String SHORT_NAME = "RsExternalLinter";

    private static final Key<Set<RsFile>> ANALYZED_FILES = Key.create("ANALYZED_FILES");

    @Override
    public void inspectionStarted(
        @NotNull InspectionManager manager,
        @NotNull GlobalInspectionContext globalContext,
        @NotNull ProblemDescriptionsProcessor problemDescriptionsProcessor
    ) {
        globalContext.putUserData(ANALYZED_FILES, ContainerUtil.newConcurrentSet());
    }

    @Override
    public void checkFile(
        @NotNull PsiFile file,
        @NotNull InspectionManager manager,
        @NotNull ProblemsHolder problemsHolder,
        @NotNull GlobalInspectionContext globalContext,
        @NotNull ProblemDescriptionsProcessor problemDescriptionsProcessor
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
        @NotNull InspectionManager manager,
        @NotNull GlobalInspectionContext globalContext,
        @NotNull ProblemDescriptionsProcessor problemDescriptionsProcessor
    ) {
        if (!(globalContext instanceof GlobalInspectionContextImpl)) return;
        GlobalInspectionContextImpl contextImpl = (GlobalInspectionContextImpl) globalContext;
        Set<RsFile> analyzedFiles = globalContext.getUserData(ANALYZED_FILES);
        if (analyzedFiles == null) return;

        com.intellij.openapi.project.Project project = manager.getProject();
        com.intellij.codeInspection.ex.InspectionProfileImpl currentProfile =
            InspectionProjectProfileManager.getInstance(project).getCurrentProfile();
        com.intellij.codeInspection.ex.InspectionToolWrapper<?, ?> toolWrapper =
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

    @NotNull
    @Override
    public String getDisplayName() {
        return RsBundle.message("external.linter");
    }

    @NotNull
    @Override
    public String getShortName() {
        return SHORT_NAME;
    }

    private static Lazy<RsExternalLinterResult> checkProjectLazily(
        @NotNull CargoProject cargoProject,
        @NotNull Disposable disposable
    ) {
        return ReadAction.compute(() -> {
            com.intellij.openapi.project.Project project = cargoProject.getProject();
            Object toolchain = RsProjectSettingsServiceUtil.getToolchain(project);
            if (toolchain == null) return null;
            return RsExternalLinterUtils.checkLazily(
                toolchain,
                project,
                disposable,
                CargoCheckArgs.forCargoProject(cargoProject)
            );
        });
    }

    private static List<ProblemDescriptor> getProblemDescriptors(
        @NotNull Set<RsFile> analyzedFiles,
        @NotNull RsExternalLinterResult annotationResult
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
        @NotNull InspectionToolPresentation presentation,
        @NotNull List<ProblemDescriptor> descriptors,
        @NotNull GlobalInspectionContext context
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
    private static ProblemDescriptor highlightInfoToProblemDescriptor(@NotNull PsiFile file, @NotNull HighlightInfo highlight) {
        PsiElement startElement = file.findElementAt(highlight.getStartOffset());
        if (startElement == null) return null;
        PsiElement endElement = file.findElementAt(highlight.getEndOffset() - 1);
        if (endElement == null) endElement = startElement;
        InspectionManager manager = InspectionManager.getInstance(file.getProject());
        String description = highlight.getDescription();
        if (description == null) description = "";
        return manager.createProblemDescriptor(
            startElement,
            endElement,
            description,
            ProblemHighlightType.GENERIC_ERROR_OR_WARNING,
            false
        );
    }

    private static RefElement getProblemElement(@NotNull PsiElement element, @NotNull GlobalInspectionContext context) {
        RsFile problemElement = RsElementUtil.ancestorOrSelf(element, RsFile.class);
        RefElement refElement = context.getRefManager().getReference(problemElement);
        if (refElement == null && problemElement != null) {
            return GlobalInspectionContextUtil.retrieveRefElement(element, context);
        } else {
            return refElement;
        }
    }
}
