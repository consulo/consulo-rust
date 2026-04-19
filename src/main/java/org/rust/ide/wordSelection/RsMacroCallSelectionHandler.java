/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.ide.wordSelection;

import com.intellij.codeInsight.editorActions.ExtendWordSelectionHandlerBase;
import com.intellij.codeInsight.editorActions.SelectWordUtil;
import com.intellij.ide.CopyProvider;
import com.intellij.ide.CutProvider;
import com.intellij.ide.DeleteProvider;
import com.intellij.ide.PasteProvider;
import com.intellij.openapi.Disposable;
import com.intellij.openapi.actionSystem.DataContext;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.editor.EditorSettings;
import com.intellij.openapi.editor.LineExtensionInfo;
import com.intellij.openapi.editor.colors.EditorColorsScheme;
import com.intellij.openapi.editor.ex.*;
import com.intellij.openapi.editor.ex.util.LexerEditorHighlighter;
import com.intellij.openapi.editor.highlighter.EditorHighlighter;
import com.intellij.openapi.editor.impl.DocumentImpl;
import com.intellij.openapi.editor.impl.ImaginaryEditor;
import com.intellij.openapi.editor.impl.TextDrawingCallback;
import com.intellij.openapi.editor.markup.TextAttributes;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.TextRange;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiElement;
import com.intellij.util.ui.ButtonlessScrollBarUI;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.rust.ide.highlight.RsHighlighter;
import org.rust.lang.core.macros.MacroExpansion;
import org.rust.lang.core.macros.RsExpandedElementUtil;
import org.rust.lang.core.psi.RsMacroArgument;
import org.rust.lang.core.psi.RsMacroCall;
import org.rust.lang.core.psi.ext.RsPossibleMacroCallUtil;
import org.rust.lang.core.psi.ext.RsPsiJavaUtil;

import java.awt.*;
import java.awt.event.KeyEvent;
import java.beans.PropertyChangeListener;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.function.IntFunction;
import javax.swing.JComponent;
import javax.swing.JScrollPane;

public class RsMacroCallSelectionHandler extends ExtendWordSelectionHandlerBase {
    @Override
    public boolean canSelect(@NotNull PsiElement e) {
        return RsPsiJavaUtil.ancestorStrict(e, RsMacroArgument.class) != null;
    }

    @Override
    public List<TextRange> select(@NotNull PsiElement e, @NotNull CharSequence editorText, int cursorOffset, @NotNull Editor editor) {
        List<PsiElement> expansionElements = RsExpandedElementUtil.findExpansionElements(e);
        if (expansionElements == null || expansionElements.isEmpty()) return null;
        PsiElement elementInExpansion = expansionElements.get(0);

        int offsetInExpansion = elementInExpansion.getTextRange().getStartOffset() + (cursorOffset - e.getTextRange().getStartOffset());

        PsiElement macroCallCandidate = RsExpandedElementUtil.findMacroCallExpandedFromNonRecursive(elementInExpansion);
        if (!(macroCallCandidate instanceof RsMacroCall)) return null;
        RsMacroCall macroCall = (RsMacroCall) macroCallCandidate;

        MacroExpansion expansion = RsPossibleMacroCallUtil.getExpansion(macroCall);
        if (expansion == null) return null;
        String expansionText = expansion.getFile().getText();

        // A real EditorImpl can't be created outside of EDT (`select` is called outside of EDT since 2020.3)
        FakeEditorEx expansionEditor = new FakeEditorEx(e.getProject(), expansionText, editor);

        List<TextRange> ranges = new ArrayList<>();
        SelectWordUtil.processRanges(elementInExpansion, expansionText, offsetInExpansion, expansionEditor, range -> {
            ranges.add(range);
            return false; // Continue processing
        });

        List<TextRange> result = new ArrayList<>();
        for (TextRange range : ranges) {
            TextRange mapped = RsExpandedElementUtil.mapRangeFromExpansionToCallBodyStrict((PsiElement) macroCall, range);
            if (mapped != null) {
                result.add(mapped);
            }
        }
        return result.isEmpty() ? null : result;
    }

    /**
     * EditorEx and Highlighter is needed for InjectedFileReferenceSelectioner, ImaginaryEditor is not enough.
     */
    private static class FakeEditorEx extends ImaginaryEditor implements EditorEx {

        private final Editor myEditor;
        private final EditorHighlighter myHighlighter;

        FakeEditorEx(@NotNull Project project, @NotNull String text, @NotNull Editor editor) {
            super(project, new DocumentImpl(text));
            myEditor = editor;
            LexerEditorHighlighter highlighter = new LexerEditorHighlighter(new RsHighlighter(), editor.getColorsScheme());
            highlighter.setText(text);
            myHighlighter = highlighter;
        }

        @NotNull
        @Override
        public DocumentEx getDocument() {
            return (DocumentEx) super.getDocument();
        }

        @NotNull
        @Override
        public EditorSettings getSettings() {
            return myEditor.getSettings();
        }

        @NotNull
        @Override
        public EditorHighlighter getHighlighter() {
            return myHighlighter;
        }

        @NotNull
        @Override
        public MarkupModelEx getMarkupModel() { throw notImplemented(); }

        @NotNull
        @Override
        public FoldingModelEx getFoldingModel() { throw notImplemented(); }

        @NotNull
        @Override
        public ScrollingModelEx getScrollingModel() { throw notImplemented(); }

        @NotNull
        @Override
        public SoftWrapModelEx getSoftWrapModel() { throw notImplemented(); }

        @NotNull
        @Override
        public MarkupModelEx getFilteredDocumentMarkupModel() { throw notImplemented(); }

        @NotNull
        @Override
        public EditorGutterComponentEx getGutterComponentEx() { throw notImplemented(); }

        @NotNull
        @Override
        public JComponent getPermanentHeaderComponent() { throw notImplemented(); }

        @Override
        public void setViewer(boolean isViewer) { throw notImplemented(); }

        @Override
        public void setPermanentHeaderComponent(@Nullable JComponent component) { throw notImplemented(); }

        @Override
        public void setHighlighter(@NotNull EditorHighlighter highlighter) { throw notImplemented(); }

        @Override
        public void setColorsScheme(@NotNull EditorColorsScheme scheme) { throw notImplemented(); }

        @Override
        public void setInsertMode(boolean val) { throw notImplemented(); }

        @Override
        public void setColumnMode(boolean val) { throw notImplemented(); }

        @Override
        public void setVerticalScrollbarOrientation(int type) { throw notImplemented(); }

        @Override
        public int getVerticalScrollbarOrientation() { throw notImplemented(); }

        @Override
        public void setVerticalScrollbarVisible(boolean b) { throw notImplemented(); }

        @Override
        public void setHorizontalScrollbarVisible(boolean b) { throw notImplemented(); }

        @NotNull
        @Override
        public CutProvider getCutProvider() { throw notImplemented(); }

        @NotNull
        @Override
        public CopyProvider getCopyProvider() { throw notImplemented(); }

        @NotNull
        @Override
        public PasteProvider getPasteProvider() { throw notImplemented(); }

        @NotNull
        @Override
        public DeleteProvider getDeleteProvider() { throw notImplemented(); }

        @Override
        public void repaint(int startOffset, int endOffset) { throw notImplemented(); }

        @Override
        public void reinitSettings() { throw notImplemented(); }

        @Override
        public void addPropertyChangeListener(@NotNull PropertyChangeListener listener, @NotNull Disposable parentDisposable) { throw notImplemented(); }

        @Override
        public void addPropertyChangeListener(@NotNull PropertyChangeListener listener) { throw notImplemented(); }

        @Override
        public void removePropertyChangeListener(@NotNull PropertyChangeListener listener) { throw notImplemented(); }

        @Override
        public int getMaxWidthInRange(int startOffset, int endOffset) { throw notImplemented(); }

        @Override
        public boolean setCaretVisible(boolean b) { throw notImplemented(); }

        @Override
        public boolean setCaretEnabled(boolean enabled) { throw notImplemented(); }

        @Override
        public void addFocusListener(@NotNull FocusChangeListener listener) { throw notImplemented(); }

        @Override
        public void addFocusListener(@NotNull FocusChangeListener listener, @NotNull Disposable parentDisposable) { throw notImplemented(); }

        @Override
        public void setOneLineMode(boolean b) { throw notImplemented(); }

        @NotNull
        @Override
        public JScrollPane getScrollPane() { throw notImplemented(); }

        @Override
        public boolean isRendererMode() { throw notImplemented(); }

        @Override
        public void setRendererMode(boolean isRendererMode) { throw notImplemented(); }

        @Override
        public void setFile(@NotNull VirtualFile vFile) { throw notImplemented(); }

        @NotNull
        @Override
        public DataContext getDataContext() { throw notImplemented(); }

        @Override
        public boolean processKeyTyped(@NotNull KeyEvent e) { throw notImplemented(); }

        @Override
        public void setFontSize(int fontSize) { throw notImplemented(); }

        @Nullable
        @Override
        public Color getBackgroundColor() { throw notImplemented(); }

        @Override
        public void setBackgroundColor(@Nullable Color color) { throw notImplemented(); }

        @NotNull
        @Override
        public Dimension getContentSize() { throw notImplemented(); }

        @Override
        public boolean isEmbeddedIntoDialogWrapper() { throw notImplemented(); }

        @Override
        public void setEmbeddedIntoDialogWrapper(boolean b) { throw notImplemented(); }

        @NotNull
        @Override
        public VirtualFile getVirtualFile() { throw notImplemented(); }

        @NotNull
        @Override
        public TextDrawingCallback getTextDrawingCallback() { throw notImplemented(); }

        @NotNull
        @Override
        public EditorColorsScheme createBoundColorSchemeDelegate(@Nullable EditorColorsScheme customGlobalScheme) { throw notImplemented(); }

        @Override
        public void setPlaceholder(@Nullable CharSequence text) { throw notImplemented(); }

        @Override
        public void setPlaceholderAttributes(@Nullable TextAttributes attributes) { throw notImplemented(); }

        @Override
        public void setShowPlaceholderWhenFocused(boolean show) { throw notImplemented(); }

        @Override
        public boolean isStickySelection() { throw notImplemented(); }

        @Override
        public void setStickySelection(boolean enable) { throw notImplemented(); }

        @Override
        public int getPrefixTextWidthInPixels() { throw notImplemented(); }

        @Override
        public void setPrefixTextAndAttributes(@Nullable String prefixText, @Nullable TextAttributes attributes) { throw notImplemented(); }

        @Override
        public boolean isPurePaintingMode() { throw notImplemented(); }

        @Override
        public void setPurePaintingMode(boolean enabled) { throw notImplemented(); }

        @Override
        public void registerLineExtensionPainter(@NotNull IntFunction<? extends Collection<? extends LineExtensionInfo>> lineExtensionPainter) { throw notImplemented(); }

        @Override
        public void registerScrollBarRepaintCallback(@Nullable ButtonlessScrollBarUI.ScrollbarRepaintCallback callback) { throw notImplemented(); }

        @Override
        public int getExpectedCaretOffset() { throw notImplemented(); }

        @Override
        public void setContextMenuGroupId(@Nullable String groupId) { throw notImplemented(); }

        @Nullable
        @Override
        public String getContextMenuGroupId() { throw notImplemented(); }

        @Override
        public void installPopupHandler(@NotNull EditorPopupHandler popupHandler) { throw notImplemented(); }

        @Override
        public void uninstallPopupHandler(@NotNull EditorPopupHandler popupHandler) { throw notImplemented(); }

        @Override
        public void setCustomCursor(@NotNull Object requestor, @Nullable Cursor cursor) { throw notImplemented(); }
    }
}
