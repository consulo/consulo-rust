package com.intellij.task;
import com.intellij.openapi.roots.ProjectModelBuildableElement;
/** Build task targeting a single project model element, optionally incremental. */
public interface ProjectModelBuildTask<T extends ProjectModelBuildableElement> extends ProjectTask {
    T getBuildableElement();
    boolean isIncrementalBuild();
}
