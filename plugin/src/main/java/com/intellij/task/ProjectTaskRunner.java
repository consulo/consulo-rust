package com.intellij.task;

import consulo.project.Project;
import consulo.util.concurrent.Promise;
import jakarta.annotation.Nonnull;

/** IntelliJ-compat stub. Consulo uses a different build task model. */
public abstract class ProjectTaskRunner {
    public abstract boolean canRun(ProjectTask task);

    public Promise<Result> run(@Nonnull Project project, @Nonnull ProjectTaskContext context, @Nonnull ProjectTask... tasks) {
        return null;
    }

    public interface Result {
        boolean isAborted();
        boolean hasErrors();
    }
}
