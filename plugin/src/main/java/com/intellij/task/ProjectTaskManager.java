package com.intellij.task;
import consulo.project.Project;
import consulo.util.concurrent.Promise;
public abstract class ProjectTaskManager {
    public static ProjectTaskManager getInstance(Project project) { return null; }
    public abstract Promise<ProjectTaskRunner.Result> run(ProjectTask... tasks);
    public abstract ProjectTask createModulesBuildTask(consulo.module.Module[] modules, boolean isIncrementalBuild, boolean includeDependentModules, boolean includeRuntimeDependencies);
    public abstract ProjectTask createAllModulesBuildTask(boolean isIncrementalBuild, Project project);
    public abstract Promise<ProjectTaskRunner.Result> build(Object buildable);
    public abstract Promise<ProjectTaskRunner.Result> buildAllModules();
}
