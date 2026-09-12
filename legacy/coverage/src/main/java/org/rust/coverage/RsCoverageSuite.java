/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.coverage;

import com.intellij.coverage.BaseCoverageSuite;
import com.intellij.coverage.CoverageEngine;
import com.intellij.coverage.CoverageFileProvider;
import com.intellij.coverage.CoverageRunner;
import com.intellij.execution.process.ProcessHandler;
import com.intellij.openapi.project.Project;
import org.jdom.Element;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

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
        @NotNull Project project,
        @NotNull String name,
        @NotNull CoverageFileProvider fileProvider,
        @NotNull CoverageRunner coverageRunner,
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

    @NotNull
    @Override
    public CoverageEngine getCoverageEngine() {
        return RsCoverageEngine.getInstance();
    }

    @Override
    public void writeExternal(@NotNull Element element) {
        super.writeExternal(element);
        if (contextFilePath != null) {
            element.setAttribute(CONTEXT_FILE_PATH, contextFilePath);
        }
    }

    @Override
    public void readExternal(@NotNull Element element) {
        super.readExternal(element);
        String value = element.getAttributeValue(CONTEXT_FILE_PATH);
        if (value != null) {
            contextFilePath = value;
        }
    }
}
