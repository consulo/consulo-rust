package com.intellij.psi.impl.source.tree.injected;
import consulo.codeEditor.Editor;
public final class InjectedLanguageEditorUtil {
    private InjectedLanguageEditorUtil() {}
    public static Editor getTopLevelEditor(Editor editor) { return editor; }
}
