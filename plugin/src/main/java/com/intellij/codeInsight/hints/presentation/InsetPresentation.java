package com.intellij.codeInsight.hints.presentation;

import consulo.colorScheme.TextAttributes;
import consulo.language.editor.inlay.InlayPresentation;
import consulo.language.editor.inlay.PresentationListener;

/** IntelliJ-compat stub — wraps another presentation with insets. */
public class InsetPresentation implements InlayPresentation {
    public InsetPresentation(InlayPresentation presentation, int left, int right, int top, int bottom) {}
    @Override public int getWidth() { return 0; }
    @Override public int getHeight() { return 0; }
    @Override public void paint(java.awt.Graphics2D g, TextAttributes attrs) {}
    @Override public void fireSizeChanged(java.awt.Dimension previous, java.awt.Dimension current) {}
    @Override public void fireContentChanged(java.awt.Rectangle area) {}
    @Override public void addListener(PresentationListener listener) {}
    @Override public void removeListener(PresentationListener listener) {}
    @Override public String toString() { return ""; }
}
