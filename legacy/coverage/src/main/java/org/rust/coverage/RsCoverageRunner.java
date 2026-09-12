/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.coverage;

import com.intellij.coverage.CoverageEngine;
import com.intellij.coverage.CoverageRunner;
import com.intellij.coverage.CoverageSuite;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.progress.ProgressManager;
import com.intellij.rt.coverage.data.LineData;
import com.intellij.rt.coverage.data.ProjectData;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.rust.RsBundle;

import java.io.File;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.List;
import java.util.Map;

public class RsCoverageRunner extends CoverageRunner {

    private static final Logger LOG = Logger.getInstance(RsCoverageRunner.class);

    @NotNull
    @Override
    public String getPresentableName() {
        return "Rust";
    }

    @NotNull
    @Override
    public String getDataFileExtension() {
        return "info";
    }

    @NotNull
    @Override
    public String getId() {
        return "RsCoverageRunner";
    }

    @Override
    public boolean acceptsCoverageEngine(@NotNull CoverageEngine engine) {
        return engine instanceof RsCoverageEngine;
    }

    @Nullable
    @Override
    public ProjectData loadCoverageData(@NotNull File sessionDataFile, @Nullable CoverageSuite baseCoverageSuite) {
        if (!(baseCoverageSuite instanceof RsCoverageSuite)) return null;
        RsCoverageSuite rsSuite = (RsCoverageSuite) baseCoverageSuite;
        try {
            if (ApplicationManager.getApplication().isDispatchThread()) {
                return org.rust.openapiext.OpenApiUtil.computeWithCancelableProgress(
                    rsSuite.getProject(),
                    RsBundle.message("progress.title.loading.coverage.data"),
                    () -> {
                        try {
                            return readProjectData(sessionDataFile, rsSuite);
                        } catch (IOException ex) {
                            throw new UncheckedIOException(ex);
                        }
                    }
                );
            } else {
                return readProjectData(sessionDataFile, rsSuite);
            }
        } catch (IOException e) {
            LOG.warn("Can't read coverage data", e);
            return null;
        } catch (UncheckedIOException e) {
            LOG.warn("Can't read coverage data", e.getCause());
            return null;
        }
    }

    @Nullable
    private static ProjectData readProjectData(@NotNull File dataFile, @NotNull RsCoverageSuite coverageSuite) throws IOException {
        var coverageProcess = coverageSuite.getCoverageProcess();
        // coverageProcess == null means that we are switching to data gathered earlier
        if (coverageProcess != null) {
            for (int i = 0; i < 100; i++) {
                ProgressManager.checkCanceled();
                if (coverageProcess.waitFor(100)) break;
            }

            if (!coverageProcess.isProcessTerminated()) {
                coverageProcess.destroyProcess();
                return null;
            }
        }

        ProjectData projectData = new ProjectData();
        LcovCoverageReport report = LcovCoverageReport.Serialization.readLcov(dataFile, coverageSuite.getContextFilePath());
        for (Map.Entry<String, List<LcovCoverageReport.LineHits>> entry : report.getRecords()) {
            String filePath = entry.getKey();
            List<LcovCoverageReport.LineHits> lineHitsList = entry.getValue();
            var classData = projectData.getOrCreateClassData(filePath);
            int max = 0;
            if (!lineHitsList.isEmpty()) {
                max = lineHitsList.get(lineHitsList.size() - 1).getLineNumber();
            }
            LineData[] lines = new LineData[max + 1];
            for (LcovCoverageReport.LineHits lineHits : lineHitsList) {
                LineData lineData = new LineData(lineHits.getLineNumber(), null);
                lineData.setHits(lineHits.getHits());
                lines[lineHits.getLineNumber()] = lineData;
            }
            classData.setLines(lines);
        }
        return projectData;
    }
}
