package com.intellij.codeHighlighting;
import consulo.project.Project;
/** IntelliJ-compat alias — Consulo has this as a nested class {@code TextEditorHighlightingPassFactory.Registrar}. */
public interface TextEditorHighlightingPassRegistrar {
    static TextEditorHighlightingPassRegistrar getInstance(Project project) { return null; }
    int registerTextEditorHighlightingPass(Object factory, int[] runAfterCompletionOf, int[] runAfterOfStartingOf, boolean runIntentionsPassAfter, int forcedPassId);
    interface Anchor {}
}
