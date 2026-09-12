package com.intellij.codeInsight.editorActions;
import consulo.codeEditor.Editor;
import consulo.language.psi.PsiFile;
import consulo.project.Project;
import java.util.List;
import consulo.document.RangeMarker;
public abstract class CopyPastePostProcessor<T extends TextBlockTransferableData> {
    public abstract List<T> collectTransferableData(PsiFile file, Editor editor, int[] startOffsets, int[] endOffsets);
    public abstract List<T> extractTransferableData(java.awt.datatransfer.Transferable content);
    public abstract void processTransferableData(Project project, Editor editor, consulo.document.RangeMarker bounds, int caretOffset, boolean[] indented, List<T> values);
}
