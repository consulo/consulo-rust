package com.intellij.codeInsight.hints;
import consulo.language.psi.PsiElement;
import consulo.codeEditor.Editor;
/** IntelliJ-compat stub. */
public interface InlayHintsCollector {
    boolean collect(PsiElement element, Editor editor, InlayHintsSink sink);
}
