package com.intellij.task.impl;
import com.intellij.task.ProjectModelBuildTask;
import com.intellij.openapi.roots.ProjectModelBuildableElement;
/** Build task for a single buildable project-model element. */
public class ProjectModelBuildTaskImpl<T extends ProjectModelBuildableElement> implements ProjectModelBuildTask<T> {
    private final T buildableElement;
    private final boolean incremental;
    public ProjectModelBuildTaskImpl(T element, boolean incremental) {
        this.buildableElement = element;
        this.incremental = incremental;
    }
    @Override public T getBuildableElement() { return buildableElement; }
    @Override public boolean isIncrementalBuild() { return incremental; }
    @Override public String getPresentableName() { return "build"; }
}
