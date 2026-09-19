/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.coverage;

import consulo.execution.coverage.CoverageEngine;
import consulo.execution.coverage.CoverageRunner;
import consulo.execution.coverage.CoverageSuite;
import consulo.application.ApplicationManager;
import consulo.logging.Logger;
import consulo.application.progress.ProgressManager;
import consulo.execution.coverage.data.CoverageLine;
import consulo.execution.coverage.data.CoverageLineImpl;
import consulo.execution.coverage.data.CoverageProjectData;
import consulo.execution.coverage.data.CoverageProjectDataImpl;
import consulo.execution.coverage.data.CoverageUnit;
import consulo.execution.coverage.data.LineStatus;
import consulo.annotation.component.ExtensionImpl;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import org.rust.RsBundle;

import java.io.File;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

@ExtensionImpl
public class RsCoverageRunner extends CoverageRunner {

    private static final Logger LOG = Logger.getInstance(RsCoverageRunner.class);

    @Nonnull
    @Override
    public String getPresentableName() {
        return "Rust";
    }

    @Nonnull
    @Override
    public String getDataFileExtension() {
        return "info";
    }

    @Nonnull
    @Override
    public String getId() {
        return "RsCoverageRunner";
    }

    @Override
    public boolean acceptsCoverageEngine(@Nonnull CoverageEngine engine) {
        return engine instanceof RsCoverageEngine;
    }

    @Nullable
    @Override
    public CoverageProjectData loadCoverageData(@Nonnull File sessionDataFile, @Nullable CoverageSuite baseCoverageSuite) {
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
    private static CoverageProjectData readProjectData(@Nonnull File dataFile, @Nonnull RsCoverageSuite coverageSuite) throws IOException {
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

        CoverageProjectData projectData = new CoverageProjectDataImpl();
        LcovCoverageReport report = LcovCoverageReport.Serialization.readLcov(dataFile, coverageSuite.getContextFilePath());
        for (Map.Entry<String, List<LcovCoverageReport.LineHits>> entry : report.getRecords()) {
            String filePath = entry.getKey();
            List<LcovCoverageReport.LineHits> lineHitsList = entry.getValue();
            CoverageUnit unit = projectData.getOrCreateUnit(filePath);
            int max = 0;
            if (!lineHitsList.isEmpty()) {
                max = lineHitsList.get(lineHitsList.size() - 1).getLineNumber();
            }
            // The list is addressed by line number, so it stays dense and holds a null per uncovered line.
            List<CoverageLine> lines = new ArrayList<>(Collections.nCopies(max + 1, null));
            for (LcovCoverageReport.LineHits lineHits : lineHitsList) {
                CoverageLineImpl line = new CoverageLineImpl(lineHits.getLineNumber(), null);
                line.setHits(lineHits.getHits());
                line.setStatus(lineHits.getHits() > 0 ? LineStatus.COVERED : LineStatus.NOT_COVERED);
                lines.set(lineHits.getLineNumber(), line);
            }
            unit.setLines(lines);
        }
        return projectData;
    }
}
