package com.intellij.codeInsight.hints;

import consulo.codeEditor.Editor;
import consulo.language.psi.PsiElement;

public abstract class FactoryInlayHintsCollector implements InlayHintsCollector {
    protected final Editor editor;
    public FactoryInlayHintsCollector(Editor editor) { this.editor = editor; }
    public com.intellij.codeInsight.hints.presentation.PresentationFactory getFactory() { return null; }
    @Override public abstract boolean collect(PsiElement element, Editor editor, InlayHintsSink sink);
}
