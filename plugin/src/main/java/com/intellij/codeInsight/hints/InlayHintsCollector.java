package com.intellij.codeInsight.hints;
import consulo.language.psi.PsiElement;
import consulo.codeEditor.Editor;
/** Collects inlay hints for a PSI element into a sink. */
public interface InlayHintsCollector {
    boolean collect(PsiElement element, Editor editor, InlayHintsSink sink);
}
