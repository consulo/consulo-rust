/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.coverage;

import consulo.ide.impl.idea.codeEditor.printing.ExportToHTMLSettings;
import consulo.annotation.component.ExtensionImpl;
import consulo.application.Application;
import consulo.execution.coverage.CoverageAnnotator;
import consulo.execution.coverage.CoverageDataManager;
import consulo.execution.coverage.CoverageEngine;
import consulo.execution.coverage.CoverageFileProvider;
import consulo.execution.coverage.CoverageRunner;
import consulo.execution.coverage.CoverageSuite;
import consulo.execution.coverage.CoverageSuitesBundle;
import consulo.execution.coverage.data.CoverageLine;
import consulo.execution.coverage.data.CoverageProjectData;
import consulo.execution.coverage.data.CoverageUnit;
import consulo.execution.coverage.view.CoverageViewExtension;
import consulo.execution.coverage.CoverageViewManager;
import consulo.execution.coverage.view.DirectoryCoverageViewExtension;
import consulo.execution.coverage.view.ElementColumnInfo;
import consulo.execution.coverage.view.PercentageCoverageColumnInfo;
import consulo.localize.LocalizeValue;
import consulo.execution.configuration.RunConfigurationBase;
import consulo.execution.coverage.CoverageEnabledConfiguration;
import consulo.execution.test.AbstractTestProxy;
import consulo.project.ui.view.tree.AbstractTreeNode;
import consulo.dataContext.DataContext;
import consulo.application.WriteAction;
import consulo.logging.Logger;
import consulo.module.Module;
import consulo.project.Project;
import consulo.ui.ex.awt.Messages;
import consulo.util.io.FileUtil;
import consulo.virtualFileSystem.util.VirtualFileUtil;
import consulo.virtualFileSystem.VirtualFile;
import consulo.language.psi.PsiElement;
import consulo.language.psi.PsiFile;
import consulo.ui.ex.awt.ColumnInfo;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import org.rust.RsBundle;
import org.rust.cargo.runconfig.command.CargoCommandConfiguration;
import org.rust.lang.RsFileType;
import org.rust.lang.core.psi.impl.RsFile;

import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;

@ExtensionImpl
public class RsCoverageEngine extends CoverageEngine {

    private static final Logger LOG = Logger.getInstance(RsCoverageEngine.class);

    @Nonnull
    @Override
    public Set<String> getQualifiedNames(@Nonnull PsiFile sourceFile) {
        String qName = getQName(sourceFile);
        if (qName != null) {
            return Set.of(qName);
        }
        return Collections.emptySet();
    }

    @Override
    public boolean acceptedByFilters(@Nonnull PsiFile psiFile, @Nonnull CoverageSuitesBundle suite) {
        return psiFile instanceof RsFile;
    }

    @Override
    public boolean coverageEditorHighlightingApplicableTo(@Nonnull PsiFile psiFile) {
        return psiFile instanceof RsFile;
    }

    @Nonnull
    @Override
    public CoverageEnabledConfiguration createCoverageEnabledConfiguration(@Nonnull RunConfigurationBase conf) {
        return new RsCoverageEnabledConfiguration(conf);
    }

    @Nullable
    @Override
    public String getQualifiedName(@Nonnull File outputFile, @Nonnull PsiFile sourceFile) {
        return getQName(sourceFile);
    }

    @Override
    public boolean includeUntouchedFileInCoverage(
        @Nonnull String qualifiedName,
        @Nonnull File outputFile,
        @Nonnull PsiFile sourceFile,
        @Nonnull CoverageSuitesBundle suite
    ) {
        return false;
    }

    @Override
    public boolean coverageProjectViewStatisticsApplicableTo(@Nonnull VirtualFile fileOrDir) {
        return !fileOrDir.isDirectory() && fileOrDir.getFileType() == RsFileType.INSTANCE;
    }

    @Nullable
    @Override
    public String getTestMethodName(@Nonnull PsiElement element, @Nonnull AbstractTestProxy testProxy) {
        return null;
    }

    @Nonnull
    @Override
    public CoverageAnnotator getCoverageAnnotator(@Nonnull Project project) {
        return RsCoverageAnnotator.getInstance(project);
    }

    @Override
    public boolean isApplicableTo(@Nonnull RunConfigurationBase conf) {
        return conf instanceof CargoCommandConfiguration;
    }

    @Nonnull
    @Override
    public CoverageSuite createEmptyCoverageSuite(@Nonnull CoverageRunner coverageRunner) {
        return new RsCoverageSuite();
    }

    @Nonnull
    @Override
    public String getPresentableText() {
        return RsBundle.message("action.rust.coverage.text");
    }

    @Nonnull
    @Override
    public CoverageViewExtension createCoverageViewExtension(
        @Nonnull Project project,
        @Nonnull CoverageSuitesBundle suiteBundle,
        @Nonnull CoverageViewManager.StateBean stateBean
    ) {
        return new DirectoryCoverageViewExtension(project, getCoverageAnnotator(project), suiteBundle, stateBean) {
            @Override
            public ColumnInfo[] createColumnInfos() {
                return new ColumnInfo[]{
                    new ElementColumnInfo(),
                    new PercentageCoverageColumnInfo(
                        1,
                        LocalizeValue.of(RsBundle.message("column.name.covered")),
                        getSuitesBundle(),
                        getStateBean()
                    )
                };
            }

            @Override
            public List<AbstractTreeNode> getChildrenNodes(AbstractTreeNode node) {
                // Only Rust sources carry coverage, and the project settings folder is never of interest.
                return super.getChildrenNodes(node).stream()
                    .filter(child -> {
                        Object value = child.getValue();
                        if (value instanceof PsiFile psiFile) {
                            return psiFile.getFileType() == RsFileType.INSTANCE;
                        }
                        return !Project.DIRECTORY_STORE_FOLDER.equals(child.getName());
                    })
                    .collect(Collectors.toList());
            }
        };
    }

    @Override
    public boolean recompileProjectAndRerunAction(
        @Nonnull Module module,
        @Nonnull CoverageSuitesBundle suite,
        @Nonnull Runnable chooseSuiteAction
    ) {
        return false;
    }

    @Override
    public boolean canHavePerTestCoverage(@Nonnull RunConfigurationBase conf) {
        return false;
    }

    @Nonnull
    @Override
    public List<PsiElement> findTestsByNames(@Nonnull String[] testNames, @Nonnull Project project) {
        return Collections.emptyList();
    }

    @Override
    public boolean isReportGenerationAvailable(
        @Nonnull Project project,
        @Nonnull DataContext dataContext,
        @Nonnull CoverageSuitesBundle currentSuite
    ) {
        return true;
    }

    @Override
    public void generateReport(
        @Nonnull Project project,
        @Nonnull DataContext dataContext,
        @Nonnull CoverageSuitesBundle currentSuiteBundle
    ) {
        LcovCoverageReport coverageReport = new LcovCoverageReport();
        CoverageDataManager dataManager = CoverageDataManager.getInstance(project);
        for (CoverageSuite suite : currentSuiteBundle.getSuites()) {
            CoverageProjectData projectData = suite.getCoverageData(dataManager);
            if (projectData == null) continue;
            for (CoverageUnit unit : projectData.getUnits()) {
                List<LcovCoverageReport.LineHits> lineHitsList = convertUnitToLineHits(unit);
                coverageReport.mergeFileReport(null, unit.getName(), lineHitsList);
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

    private void refresh(@Nonnull File file) {
        VirtualFile vFile = VirtualFileUtil.findFileByIoFile(file, true);
        if (vFile != null) {
            WriteAction.runAndWait(() -> vFile.refresh(false, false));
        }
    }

    @Nonnull
    private String getOutputFileName(@Nonnull CoverageSuitesBundle currentSuitesBundle) {
        StringBuilder sb = new StringBuilder();
        for (CoverageSuite suite : currentSuitesBundle.getSuites()) {
            String presentableName = suite.getPresentableName();
            sb.append(presentableName);
        }
        sb.append(".lcov");
        return sb.toString();
    }

    @Nonnull
    private List<LcovCoverageReport.LineHits> convertUnitToLineHits(@Nonnull CoverageUnit unit) {
        List<CoverageLine> lines = unit.getLines();
        List<LcovCoverageReport.LineHits> lineHitsList = new ArrayList<>(lines.size());
        for (CoverageLine line : lines) {
            // The list is addressed by line number, so uncovered lines are held as nulls.
            if (line != null) {
                lineHitsList.add(new LcovCoverageReport.LineHits(line.getLineNumber(), line.getHits()));
            }
        }
        return lineHitsList;
    }

    @Nullable
    @Override
    public List<Integer> collectSrcLinesForUntouchedFile(@Nonnull File classFile, @Nonnull CoverageSuitesBundle suite) {
        return null;
    }

    @Nullable
    @Override
    public CoverageSuite createCoverageSuite(
        @Nonnull CoverageRunner covRunner,
        @Nonnull String name,
        @Nonnull CoverageFileProvider coverageDataFileProvider,
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
        @Nonnull CoverageRunner covRunner,
        @Nonnull String name,
        @Nonnull CoverageFileProvider coverageDataFileProvider,
        @Nonnull CoverageEnabledConfiguration config
    ) {
        if (!(config instanceof RsCoverageEnabledConfiguration)) return null;
        RsCoverageEnabledConfiguration rsConfig = (RsCoverageEnabledConfiguration) config;
        RunConfigurationBase configuration = config.getConfiguration();
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

    @Nonnull
    public static RsCoverageEngine getInstance() {
        return Application.get().getExtensionPoint(CoverageEngine.class).findExtensionOrFail(RsCoverageEngine.class);
    }

    @Nullable
    private static String getQName(@Nonnull PsiFile sourceFile) {
        VirtualFile vFile = sourceFile.getVirtualFile();
        return vFile != null ? vFile.getPath() : null;
    }
}
