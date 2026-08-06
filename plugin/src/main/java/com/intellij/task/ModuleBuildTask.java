package com.intellij.task;
import consulo.module.Module;
/** IntelliJ-compat stub. */
public interface ModuleBuildTask extends ProjectTask {
    Module getModule();
    boolean isIncrementalBuild();
    boolean isIncludeDependentModules();
    boolean isIncludeRuntimeDependencies();
    boolean isIncludeTests();
}
