package com.intellij.task.impl;
import com.intellij.task.ProjectTask;
import com.intellij.openapi.roots.ProjectModelBuildableElement;
/** IntelliJ-compat stub. */
public class ProjectModelBuildTaskImpl<T extends ProjectModelBuildableElement> implements ProjectTask {
    private final T buildableElement;
    private final boolean incremental;
    public ProjectModelBuildTaskImpl(T element, boolean incremental) {
        this.buildableElement = element;
        this.incremental = incremental;
    }
    public T getBuildableElement() { return buildableElement; }
    public boolean isIncrementalBuild() { return incremental; }
    @Override public String getPresentableName() { return "build"; }
}
