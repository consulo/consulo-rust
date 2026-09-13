package com.intellij.task;

import consulo.execution.configuration.RunConfiguration;
import jakarta.annotation.Nullable;

/** Context of a project task, carrying the run configuration that triggered it. */
public class ProjectTaskContext {
    public ProjectTaskContext() {}

    @Nullable
    public RunConfiguration getRunConfiguration() {
        return null;
    }
}
