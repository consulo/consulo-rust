package com.intellij.openapi.editor.impl;

import consulo.codeEditor.SoftWrap;
import consulo.codeEditor.SoftWrapModel;
import consulo.codeEditor.VisualPosition;

import java.util.Collections;
import java.util.List;

/** IntelliJ-compat stub — empty soft-wrap model. */
public class EmptySoftWrapModel implements SoftWrapModel {
    @Override public boolean isSoftWrappingEnabled() { return false; }
    @Override public SoftWrap getSoftWrap(int offset) { return null; }
    @Override public List<? extends SoftWrap> getSoftWrapsForRange(int start, int end) { return Collections.emptyList(); }
    @Override public List<? extends SoftWrap> getSoftWrapsForLine(int documentLine) { return Collections.emptyList(); }
    @Override public boolean isVisible(SoftWrap softWrap) { return false; }
    @Override public void beforeDocumentChangeAtCaret() {}
    @Override public boolean isInsideSoftWrap(VisualPosition position) { return false; }
    @Override public boolean isInsideOrBeforeSoftWrap(VisualPosition visual) { return false; }
    @Override public void release() {}
}
