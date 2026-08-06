package com.intellij.openapi.editor.impl;

import consulo.codeEditor.CaretModel;
import consulo.codeEditor.Editor;
import consulo.colorScheme.EditorColorsScheme;
import consulo.codeEditor.EditorGutter;
import consulo.codeEditor.FoldingModel;
import consulo.codeEditor.LogicalPosition;
import consulo.codeEditor.ScrollingModel;
import consulo.codeEditor.SelectionModel;
import consulo.codeEditor.SoftWrapModel;
import consulo.codeEditor.VisualPosition;
import consulo.codeEditor.EditorKind;
import consulo.codeEditor.markup.MarkupModel;
import consulo.colorScheme.TextAttributes;
import consulo.codeEditor.event.EditorMouseEventArea;
import consulo.codeEditor.event.EditorMouseListener;
import consulo.codeEditor.event.EditorMouseMotionListener;
import consulo.codeEditor.event.CaretListener;
import consulo.codeEditor.event.VisibleAreaListener;
import consulo.document.Document;
import consulo.project.Project;
import consulo.util.dataholder.Key;

import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Point;
import java.awt.event.KeyEvent;
import java.awt.event.MouseEvent;

/**
 * IntelliJ-compat stub: an in-memory read-only Editor without a UI.
 * Subclasses override methods they actually need; the rest throw
 * {@link #notImplemented()}.
 */
public class ImaginaryEditor implements Editor {
    private final Project project;
    private final Document document;
    public ImaginaryEditor(Project project, Document document) {
        this.project = project;
        this.document = document;
    }
    @Override public Project getProject() { return project; }
    @Override public Document getDocument() { return document; }
    @Override public boolean isViewer() { return true; }
    @Override public SelectionModel getSelectionModel() { throw notImplemented(); }
    @Override public MarkupModel getMarkupModel() { throw notImplemented(); }
    @Override public FoldingModel getFoldingModel() { throw notImplemented(); }
    @Override public ScrollingModel getScrollingModel() { throw notImplemented(); }
    @Override public CaretModel getCaretModel() { throw notImplemented(); }
    @Override public SoftWrapModel getSoftWrapModel() { throw notImplemented(); }
    @Override public EditorGutter getGutter() { throw notImplemented(); }
    @Override public EditorColorsScheme getColorsScheme() { throw notImplemented(); }
    @Override public int getLineHeight() { return 0; }
    @Override public Point logicalPositionToXY(LogicalPosition pos) { throw notImplemented(); }
    @Override public int logicalPositionToOffset(LogicalPosition pos) { throw notImplemented(); }
    @Override public VisualPosition logicalToVisualPosition(LogicalPosition logicalPos) { throw notImplemented(); }
    @Override public Point visualPositionToXY(VisualPosition visible) { throw notImplemented(); }
    @Override public Point2D visualPositionToPoint2D(VisualPosition pos) { throw notImplemented(); }
    @Override public LogicalPosition visualToLogicalPosition(VisualPosition visiblePos) { throw notImplemented(); }
    @Override public LogicalPosition offsetToLogicalPosition(int offset) { throw notImplemented(); }
    @Override public VisualPosition offsetToVisualPosition(int offset) { throw notImplemented(); }
    @Override public VisualPosition offsetToVisualPosition(int offset, boolean leanForward, boolean beforeSoftWrap) { throw notImplemented(); }
    @Override public LogicalPosition xyToLogicalPosition(Point p) { throw notImplemented(); }
    @Override public VisualPosition xyToVisualPosition(Point p) { throw notImplemented(); }
    @Override public VisualPosition xyToVisualPosition(java.awt.geom.Point2D p) { throw notImplemented(); }
    @Override public void addEditorMouseListener(EditorMouseListener listener) { throw notImplemented(); }
    @Override public void removeEditorMouseListener(EditorMouseListener listener) { throw notImplemented(); }
    @Override public void addEditorMouseMotionListener(EditorMouseMotionListener listener) { throw notImplemented(); }
    @Override public void removeEditorMouseMotionListener(EditorMouseMotionListener listener) { throw notImplemented(); }
    @Override public boolean isDisposed() { return false; }
    @Override public boolean isInsertMode() { return false; }
    @Override public boolean isColumnMode() { return false; }
    @Override public boolean isOneLineMode() { return false; }
    @Override public EditorMouseEventArea getMouseEventArea(MouseEvent e) { return null; }
    @Override public void setHeaderComponent(javax.swing.JComponent header) {}
    @Override public boolean hasHeaderComponent() { return false; }
    @Override public javax.swing.JComponent getHeaderComponent() { return null; }
    @Override public <T> T getUserData(Key<T> key) { return null; }
    @Override public <T> void putUserData(Key<T> key, T value) {}
    @Override public EditorKind getEditorKind() { return EditorKind.UNTYPED; }
    @Override public consulo.dataContext.DataContext getDataContext() { throw notImplemented(); }
    @Override public consulo.codeEditor.InlayModel getInlayModel() { throw notImplemented(); }
    @Override public consulo.codeEditor.IndentsModel getIndentsModel() { throw notImplemented(); }
    @Override public consulo.codeEditor.EditorSettings getSettings() { throw notImplemented(); }

    protected java.awt.geom.Point2D.Double Point2D() { return null; }
    public java.awt.geom.Point2D.Double Point2Dr() { return null; }

    protected RuntimeException notImplemented() { return new UnsupportedOperationException("not implemented on ImaginaryEditor"); }

    // helper for awt.geom.Point2D
    public static class Point2D extends java.awt.geom.Point2D.Double {}
}
