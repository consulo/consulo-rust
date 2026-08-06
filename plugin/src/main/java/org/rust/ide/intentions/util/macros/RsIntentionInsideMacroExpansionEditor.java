/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.ide.intentions.util.macros;

import consulo.codeEditor.Editor;
import consulo.codeEditor.CaretModel;
import consulo.codeEditor.EditorFactory;
import consulo.codeEditor.SelectionModel;
import consulo.codeEditor.ScrollType;
import consulo.codeEditor.EditorEx;
import consulo.codeEditor.EditorSettings;
import consulo.codeEditor.SoftWrapModel;
import consulo.codeEditor.LogicalPosition;
import consulo.codeEditor.VisualPosition;
import consulo.document.Document;
import consulo.document.RangeMarker;
import com.intellij.openapi.editor.impl.EmptySoftWrapModel;
import com.intellij.openapi.editor.impl.ImaginaryEditor;
import consulo.language.psi.PsiFile;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

public class RsIntentionInsideMacroExpansionEditor extends ImaginaryEditor {
    private final PsiFile myPsiFileCopy;
    private final PsiFile myOriginalFile;
    private final Editor myOriginalEditor;
    private final Integer myInitialMappedOffset;
    private final RsIntentionInsideMacroExpansionContext myContext;

    public RsIntentionInsideMacroExpansionEditor(
        PsiFile psiFileCopy,
        PsiFile originalFile,
        Editor originalEditor,
        @Nullable Integer initialMappedOffset,
        @Nullable RsIntentionInsideMacroExpansionContext context
    ) {
        super(psiFileCopy.getProject(), psiFileCopy.getViewProvider().getDocument());
        this.myPsiFileCopy = psiFileCopy;
        this.myOriginalFile = originalFile;
        this.myOriginalEditor = originalEditor;
        this.myInitialMappedOffset = initialMappedOffset;
        this.myContext = context;

        if (initialMappedOffset != null) {
            getCaretModel().moveToOffset(initialMappedOffset);
        }
    }

    public PsiFile getPsiFileCopy() {
        return myPsiFileCopy;
    }

    public PsiFile getOriginalFile() {
        return myOriginalFile;
    }

    public Editor getOriginalEditor() {
        return myOriginalEditor;
    }

    @Nullable
    public Integer getInitialMappedOffset() {
        return myInitialMappedOffset;
    }

    @Nullable
    public RsIntentionInsideMacroExpansionContext getContext() {
        return myContext;
    }

    @Nonnull
    @Override
    protected RuntimeException notImplemented() {
        return new IntentionInsideMacroExpansionEditorUnsupportedOperationException();
    }

    @Override
    public boolean isViewer() {
        return true;
    }

    @Override
    public boolean isOneLineMode() {
        return false;
    }

    @Nonnull
    @Override
    public EditorSettings getSettings() {
        return myOriginalEditor.getSettings();
    }

    @Override
    public int logicalPositionToOffset(@Nonnull LogicalPosition pos) {
        Document document = getDocument();
        int lineStart = document.getLineStartOffset(pos.line);
        int lineEnd = document.getLineEndOffset(pos.line);
        return Math.min(lineEnd, lineStart + pos.column);
    }

    @Nonnull
    @Override
    public VisualPosition logicalToVisualPosition(@Nonnull LogicalPosition logicalPos) {
        return new VisualPosition(logicalPos.line, logicalPos.column);
    }

    @Nonnull
    @Override
    public LogicalPosition visualToLogicalPosition(@Nonnull VisualPosition visiblePos) {
        return new LogicalPosition(visiblePos.line, visiblePos.column);
    }

    @Nonnull
    @Override
    public LogicalPosition offsetToLogicalPosition(int offset) {
        int clamped = Math.max(0, Math.min(offset, getDocument().getTextLength()));
        Document document = getDocument();
        int line = document.getLineNumber(clamped);
        int col = clamped - document.getLineStartOffset(line);
        return new LogicalPosition(line, col);
    }

    @Nonnull
    @Override
    public SoftWrapModel getSoftWrapModel() {
        return new EmptySoftWrapModel();
    }
}
