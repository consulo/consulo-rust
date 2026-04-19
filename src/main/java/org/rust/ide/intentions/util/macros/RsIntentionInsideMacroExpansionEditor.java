/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.ide.intentions.util.macros;

import com.intellij.openapi.editor.*;
import com.intellij.openapi.editor.impl.EmptySoftWrapModel;
import com.intellij.openapi.editor.impl.ImaginaryEditor;
import com.intellij.psi.PsiFile;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

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

    @NotNull
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

    @NotNull
    @Override
    public EditorSettings getSettings() {
        return myOriginalEditor.getSettings();
    }

    @Override
    public int logicalPositionToOffset(@NotNull LogicalPosition pos) {
        Document document = getDocument();
        int lineStart = document.getLineStartOffset(pos.line);
        int lineEnd = document.getLineEndOffset(pos.line);
        return Math.min(lineEnd, lineStart + pos.column);
    }

    @NotNull
    @Override
    public VisualPosition logicalToVisualPosition(@NotNull LogicalPosition logicalPos) {
        return new VisualPosition(logicalPos.line, logicalPos.column);
    }

    @NotNull
    @Override
    public LogicalPosition visualToLogicalPosition(@NotNull VisualPosition visiblePos) {
        return new LogicalPosition(visiblePos.line, visiblePos.column);
    }

    @NotNull
    @Override
    public LogicalPosition offsetToLogicalPosition(int offset) {
        int clamped = Math.max(0, Math.min(offset, getDocument().getTextLength()));
        Document document = getDocument();
        int line = document.getLineNumber(clamped);
        int col = clamped - document.getLineStartOffset(line);
        return new LogicalPosition(line, col);
    }

    @NotNull
    @Override
    public SoftWrapModel getSoftWrapModel() {
        return new EmptySoftWrapModel();
    }
}
