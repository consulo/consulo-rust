/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.coverage;

import consulo.execution.coverage.BaseCoverageSuite;
import consulo.execution.coverage.CoverageEngine;
import consulo.execution.coverage.CoverageFileProvider;
import consulo.execution.coverage.CoverageRunner;
import consulo.process.ProcessHandler;
import consulo.project.Project;
import org.jdom.Element;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

public class RsCoverageSuite extends BaseCoverageSuite {

    private static final String CONTEXT_FILE_PATH = "CONTEXT_FILE_PATH";

    @Nullable
    private String contextFilePath;

    @Nullable
    private final ProcessHandler coverageProcess;

    public RsCoverageSuite() {
        super();
        this.contextFilePath = null;
        this.coverageProcess = null;
    }

    public RsCoverageSuite(
        @Nonnull Project project,
        @Nonnull String name,
        @Nonnull CoverageFileProvider fileProvider,
        @Nonnull CoverageRunner coverageRunner,
        @Nullable String contextFilePath,
        @Nullable ProcessHandler coverageProcess
    ) {
        super(name, fileProvider, System.currentTimeMillis(), false, false, false, coverageRunner, project);
        this.contextFilePath = contextFilePath;
        this.coverageProcess = coverageProcess;
    }

    @Nullable
    public String getContextFilePath() {
        return contextFilePath;
    }

    @Nullable
    public ProcessHandler getCoverageProcess() {
        return coverageProcess;
    }

    @Nonnull
    @Override
    public CoverageEngine getCoverageEngine() {
        return RsCoverageEngine.getInstance();
    }

    @Override
    public void writeExternal(@Nonnull Element element) {
        super.writeExternal(element);
        if (contextFilePath != null) {
            element.setAttribute(CONTEXT_FILE_PATH, contextFilePath);
        }
    }

    @Override
    public void readExternal(@Nonnull Element element) {
        super.readExternal(element);
        String value = element.getAttributeValue(CONTEXT_FILE_PATH);
        if (value != null) {
            contextFilePath = value;
        }
    }
}
