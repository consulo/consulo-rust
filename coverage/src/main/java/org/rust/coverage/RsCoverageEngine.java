/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.coverage;

import com.intellij.codeEditor.printing.ExportToHTMLSettings;
import com.intellij.coverage.*;
import com.intellij.coverage.view.CoverageViewExtension;
import com.intellij.coverage.view.CoverageViewManager;
import com.intellij.coverage.view.DirectoryCoverageViewExtension;
import com.intellij.coverage.view.PercentageCoverageColumnInfo;
import com.intellij.execution.configurations.RunConfigurationBase;
import com.intellij.execution.configurations.coverage.CoverageEnabledConfiguration;
import com.intellij.execution.testframework.AbstractTestProxy;
import com.intellij.ide.util.treeView.AbstractTreeNode;
import com.intellij.ide.util.treeView.AlphaComparator;
import com.intellij.ide.util.treeView.NodeDescriptor;
import com.intellij.openapi.actionSystem.DataContext;
import com.intellij.openapi.application.WriteAction;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.Messages;
import com.intellij.openapi.util.io.FileUtil;
import com.intellij.openapi.vfs.VfsUtil;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.intellij.rt.coverage.data.ClassData;
import com.intellij.util.ui.ColumnInfo;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.rust.RsBundle;
import org.rust.cargo.runconfig.command.CargoCommandConfiguration;
import org.rust.lang.RsFileType;
import org.rust.lang.core.psi.RsFile;

import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;

public class RsCoverageEngine extends CoverageEngine {

    private static final Logger LOG = Logger.getInstance(RsCoverageEngine.class);

    @NotNull
    @Override
    public Set<String> getQualifiedNames(@NotNull PsiFile sourceFile) {
        String qName = getQName(sourceFile);
        if (qName != null) {
            return Set.of(qName);
        }
        return Collections.emptySet();
    }

    @Override
    public boolean acceptedByFilters(@NotNull PsiFile psiFile, @NotNull CoverageSuitesBundle suite) {
        return psiFile instanceof RsFile;
    }

    @Override
    public boolean coverageEditorHighlightingApplicableTo(@NotNull PsiFile psiFile) {
        return psiFile instanceof RsFile;
    }

    @NotNull
    @Override
    public CoverageEnabledConfiguration createCoverageEnabledConfiguration(@NotNull RunConfigurationBase<?> conf) {
        return new RsCoverageEnabledConfiguration(conf);
    }

    @Nullable
    @Override
    public String getQualifiedName(@NotNull File outputFile, @NotNull PsiFile sourceFile) {
        return getQName(sourceFile);
    }

    @Override
    public boolean includeUntouchedFileInCoverage(
        @NotNull String qualifiedName,
        @NotNull File outputFile,
        @NotNull PsiFile sourceFile,
        @NotNull CoverageSuitesBundle suite
    ) {
        return false;
    }

    @Override
    public boolean coverageProjectViewStatisticsApplicableTo(@NotNull VirtualFile fileOrDir) {
        return !fileOrDir.isDirectory() && fileOrDir.getFileType() == RsFileType.INSTANCE;
    }

    @Nullable
    @Override
    public String getTestMethodName(@NotNull PsiElement element, @NotNull AbstractTestProxy testProxy) {
        return null;
    }

    @NotNull
    @Override
    public CoverageAnnotator getCoverageAnnotator(@NotNull Project project) {
        return RsCoverageAnnotator.getInstance(project);
    }

    @Override
    public boolean isApplicableTo(@NotNull RunConfigurationBase<?> conf) {
        return conf instanceof CargoCommandConfiguration;
    }

    @NotNull
    @Override
    public CoverageSuite createEmptyCoverageSuite(@NotNull CoverageRunner coverageRunner) {
        return new RsCoverageSuite();
    }

    @NotNull
    @Override
    public String getPresentableText() {
        return RsBundle.message("action.rust.coverage.text");
    }

    @NotNull
    @Override
    public CoverageViewExtension createCoverageViewExtension(
        @NotNull Project project,
        @NotNull CoverageSuitesBundle suiteBundle,
        @NotNull CoverageViewManager.StateBean stateBean
    ) {
        return new DirectoryCoverageViewExtension(project, getCoverageAnnotator(project), suiteBundle, stateBean) {
            @NotNull
            @Override
            public ColumnInfo<NodeDescriptor<?>, String>[] createColumnInfos() {
                PercentageCoverageColumnInfo percentage = new PercentageCoverageColumnInfo(
                    1,
                    RsBundle.message("column.name.covered"),
                    mySuitesBundle,
                    myStateBean
                );
                ColumnInfo<NodeDescriptor<?>, String> files = new ColumnInfo<>(RsBundle.message("column.name.file")) {
                    @Override
                    public String valueOf(NodeDescriptor<?> item) {
                        return item != null ? item.toString() : "";
                    }

                    @Override
                    public Comparator<NodeDescriptor<?>> getComparator() {
                        return AlphaComparator.INSTANCE;
                    }
                };
                @SuppressWarnings("unchecked")
                ColumnInfo<NodeDescriptor<?>, String>[] result = new ColumnInfo[]{files, percentage};
                return result;
            }

            @NotNull
            @Override
            @SuppressWarnings({"unchecked", "rawtypes"})
            public List<AbstractTreeNode<?>> getChildrenNodes(AbstractTreeNode node) {
                return ((List<AbstractTreeNode<?>>) (List) super.getChildrenNodes(node)).stream()
                    .filter(child -> {
                        Object value = child.getValue();
                        if (value instanceof PsiFile) {
                            return ((PsiFile) value).getFileType() == RsFileType.INSTANCE;
                        } else {
                            return !Project.DIRECTORY_STORE_FOLDER.equals(child.getName());
                        }
                    })
                    .collect(Collectors.toList());
            }
        };
    }

    @Override
    public boolean recompileProjectAndRerunAction(
        @NotNull Module module,
        @NotNull CoverageSuitesBundle suite,
        @NotNull Runnable chooseSuiteAction
    ) {
        return false;
    }

    @Override
    public boolean canHavePerTestCoverage(@NotNull RunConfigurationBase<?> conf) {
        return false;
    }

    @NotNull
    @Override
    public List<PsiElement> findTestsByNames(@NotNull String[] testNames, @NotNull Project project) {
        return Collections.emptyList();
    }

    @Override
    public boolean isReportGenerationAvailable(
        @NotNull Project project,
        @NotNull DataContext dataContext,
        @NotNull CoverageSuitesBundle currentSuite
    ) {
        return true;
    }

    @Override
    public void generateReport(
        @NotNull Project project,
        @NotNull DataContext dataContext,
        @NotNull CoverageSuitesBundle currentSuiteBundle
    ) {
        LcovCoverageReport coverageReport = new LcovCoverageReport();
        CoverageDataManager dataManager = CoverageDataManager.getInstance(project);
        for (CoverageSuite suite : currentSuiteBundle.getSuites()) {
            var projectData = suite.getCoverageData(dataManager);
            if (projectData == null) continue;
            Map<String, ClassData> classDataMap = projectData.getClasses();
            for (Map.Entry<String, ClassData> entry : classDataMap.entrySet()) {
                String filePath = entry.getKey();
                ClassData classData = entry.getValue();
                List<LcovCoverageReport.LineHits> lineHitsList = convertClassDataToLineHits(classData);
                coverageReport.mergeFileReport(null, filePath, lineHitsList);
            }
        }

        ExportToHTMLSettings settings = ExportToHTMLSettings.getInstance(project);
        File outputDir = new File(settings.OUTPUT_DIRECTORY);
        FileUtil.createDirectory(outputDir);
        String outputFileName = getOutputFileName(currentSuiteBundle);
        String title = RsBundle.message("dialog.title.coverage.report.generation");
        try {
            File output = new File(outputDir, outputFileName);
            LcovCoverageReport.Serialization.writeLcov(coverageReport, output);
            refresh(output);
            // TODO: generate html report ourselves
            String url = "https://github.com/linux-test-project/lcov";
            Messages.showInfoMessage(
                RsBundle.message(
                    "dialog.message.html.coverage.report.has.been.successfully.saved.as.file.br.use.instruction.in.href.to.generate.html.output.html",
                    outputFileName, url, url
                ),
                title
            );
        } catch (IOException e) {
            LOG.warn("Can not export coverage data", e);
            Messages.showErrorDialog(
                RsBundle.message(
                    "dialog.message.can.not.generate.coverage.report",
                    e.getMessage() != null ? e.getMessage() : ""
                ),
                title
            );
        }
    }

    private void refresh(@NotNull File file) {
        VirtualFile vFile = VfsUtil.findFileByIoFile(file, true);
        if (vFile != null) {
            WriteAction.runAndWait(() -> vFile.refresh(false, false));
        }
    }

    @NotNull
    private String getOutputFileName(@NotNull CoverageSuitesBundle currentSuitesBundle) {
        StringBuilder sb = new StringBuilder();
        for (CoverageSuite suite : currentSuitesBundle.getSuites()) {
            String presentableName = suite.getPresentableName();
            sb.append(presentableName);
        }
        sb.append(".lcov");
        return sb.toString();
    }

    @NotNull
    private List<LcovCoverageReport.LineHits> convertClassDataToLineHits(@NotNull ClassData classData) {
        Object[] linesObj = classData.getLines();
        int lineCount = linesObj.length;
        List<LcovCoverageReport.LineHits> lineHitsList = new ArrayList<>(lineCount);
        for (int lineInd = 0; lineInd < lineCount; lineInd++) {
            var lineData = classData.getLineData(lineInd);
            if (lineData != null) {
                LcovCoverageReport.LineHits lineHits = new LcovCoverageReport.LineHits(lineData.getLineNumber(), lineData.getHits());
                lineHitsList.add(lineHits);
            }
        }
        return lineHitsList;
    }

    @Nullable
    @Override
    public List<Integer> collectSrcLinesForUntouchedFile(@NotNull File classFile, @NotNull CoverageSuitesBundle suite) {
        return null;
    }

    @Nullable
    @Override
    public CoverageSuite createCoverageSuite(
        @NotNull CoverageRunner covRunner,
        @NotNull String name,
        @NotNull CoverageFileProvider coverageDataFileProvider,
        @Nullable String[] filters,
        long lastCoverageTimeStamp,
        @Nullable String suiteToMerge,
        boolean coverageByTestEnabled,
        boolean tracingEnabled,
        boolean trackTestFolders,
        @Nullable Project project
    ) {
        return null;
    }

    @Nullable
    @Override
    public CoverageSuite createCoverageSuite(
        @NotNull CoverageRunner covRunner,
        @NotNull String name,
        @NotNull CoverageFileProvider coverageDataFileProvider,
        @NotNull CoverageEnabledConfiguration config
    ) {
        if (!(config instanceof RsCoverageEnabledConfiguration)) return null;
        RsCoverageEnabledConfiguration rsConfig = (RsCoverageEnabledConfiguration) config;
        RunConfigurationBase<?> configuration = config.getConfiguration();
        if (!(configuration instanceof CargoCommandConfiguration)) return null;
        CargoCommandConfiguration cargoConfig = (CargoCommandConfiguration) configuration;
        String workingDir = cargoConfig.getWorkingDirectory() != null
            ? cargoConfig.getWorkingDirectory().toString()
            : null;
        return new RsCoverageSuite(
            cargoConfig.getProject(),
            name,
            coverageDataFileProvider,
            covRunner,
            workingDir,
            rsConfig.coverageProcess
        );
    }

    @NotNull
    public static RsCoverageEngine getInstance() {
        return EP_NAME.findExtensionOrFail(RsCoverageEngine.class);
    }

    @Nullable
    private static String getQName(@NotNull PsiFile sourceFile) {
        VirtualFile vFile = sourceFile.getVirtualFile();
        return vFile != null ? vFile.getPath() : null;
    }
}
