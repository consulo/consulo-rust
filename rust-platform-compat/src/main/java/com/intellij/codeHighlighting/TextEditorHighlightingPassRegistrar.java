package com.intellij.codeHighlighting;
import consulo.project.Project;
/** Assigns ids to text editor highlighting passes and records the passes they must run after. */
public interface TextEditorHighlightingPassRegistrar {
    static TextEditorHighlightingPassRegistrar getInstance(Project project) { return null; }
    int registerTextEditorHighlightingPass(Object factory, int[] runAfterCompletionOf, int[] runAfterOfStartingOf, boolean runIntentionsPassAfter, int forcedPassId);
    interface Anchor {}
}
