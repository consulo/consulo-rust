package com.intellij.ui;

import consulo.codeEditor.EditorEx;

import java.awt.Color;
import java.util.List;
import java.util.function.Function;

/** IntelliJ-compat stub — expandable inline-editor support with popup expansion. */
public class ExpandableEditorSupport {
    public ExpandableEditorSupport(Object editor, Function<String, List<String>> onShow, Function<List<String>, String> onHide) {}

    /** Overridable in subclasses for fancy content wrappers. */
    public static class Content {
        public Content() {}
    }

    protected void initPopupEditor(EditorEx popupEditor, Color background) {}
    protected void initFieldEditor(EditorEx fieldEditor, Color background) {}
    protected Content buildContent(EditorEx popupEditor) { return new Content(); }
    protected void installFocusTracker() {}
}
