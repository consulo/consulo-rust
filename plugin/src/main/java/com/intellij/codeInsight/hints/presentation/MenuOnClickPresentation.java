package com.intellij.codeInsight.hints.presentation;

import consulo.colorScheme.TextAttributes;
import consulo.language.editor.inlay.InlayPresentation;
import consulo.language.editor.inlay.PresentationListener;

/** Wraps another inlay presentation, showing a context menu when it is clicked. */
public class MenuOnClickPresentation implements InlayPresentation {
    private final InlayPresentation delegate;

    public MenuOnClickPresentation(Object presentation, Object project, java.util.function.Supplier<?> actionsProvider) {
        this.delegate = presentation instanceof InlayPresentation ? (InlayPresentation) presentation : null;
    }

    @Override public int getWidth() { return delegate != null ? delegate.getWidth() : 0; }
    @Override public int getHeight() { return delegate != null ? delegate.getHeight() : 0; }
    @Override public void paint(java.awt.Graphics2D g, TextAttributes attrs) {
        if (delegate != null) delegate.paint(g, attrs);
    }
    @Override public void fireSizeChanged(java.awt.Dimension previous, java.awt.Dimension current) {}
    @Override public void fireContentChanged(java.awt.Rectangle area) {}
    @Override public void addListener(PresentationListener listener) {}
    @Override public void removeListener(PresentationListener listener) {}
    @Override public String toString() { return delegate != null ? delegate.toString() : ""; }
}
