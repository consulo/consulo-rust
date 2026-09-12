package com.intellij.task;
import consulo.module.Module;
/** A build task scoped to a single module, describing what to include in the build. */
public interface ModuleBuildTask extends ProjectTask {
    Module getModule();
    boolean isIncrementalBuild();
    boolean isIncludeDependentModules();
    boolean isIncludeRuntimeDependencies();
    boolean isIncludeTests();
}
