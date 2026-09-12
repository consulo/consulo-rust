package com.intellij.codeInsight.hints;
import consulo.codeEditor.Inlay;
/** Sink collecting the inline and block inlay presentations produced for a file. */
public interface InlayHintsSink {
    default void addInlineElement(int offset, boolean relatesToPrecedingText, Object presentation) {}
    default void addInlineElement(int offset, boolean relatesToPrecedingText, Object presentation, boolean placeAtTheEndOfLine) {}
    default void addBlockElement(int offset, boolean relatesToPrecedingText, boolean showAbove, int priority, Object presentation) {}
}
