package com.intellij.task;
import com.intellij.openapi.roots.ProjectModelBuildableElement;
/** IntelliJ-compat stub. */
public interface ProjectModelBuildTask<T extends ProjectModelBuildableElement> extends ProjectTask {
    T getBuildableElement();
    boolean isIncrementalBuild();
}
