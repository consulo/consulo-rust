package com.intellij.codeHighlighting;
import consulo.project.Project;
public interface TextEditorHighlightingPassFactoryRegistrar {
    void registerHighlightingPassFactory(TextEditorHighlightingPassRegistrar registrar, Project project);
}
