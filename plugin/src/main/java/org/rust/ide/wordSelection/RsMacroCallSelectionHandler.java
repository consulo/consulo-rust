/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.ide.wordSelection;

import consulo.annotation.component.ExtensionImpl;
import consulo.codeEditor.EditorPopupHandler;
import consulo.codeEditor.event.FocusChangeListener;
import consulo.codeEditor.markup.MarkupModelEx;
import consulo.codeEditor.FoldingModelEx;
import consulo.codeEditor.ScrollingModelEx;
import consulo.codeEditor.SoftWrapModelEx;
import consulo.codeEditor.EditorGutterComponentEx;

import consulo.language.editor.action.ExtendWordSelectionHandlerBase;
import consulo.language.editor.action.SelectWordUtil;
import consulo.ui.ex.CopyProvider;
import consulo.ui.ex.CutProvider;
import consulo.ui.ex.DeleteProvider;
import consulo.ui.ex.PasteProvider;
import consulo.disposer.Disposable;
import consulo.dataContext.DataContext;
import consulo.codeEditor.Editor;
import consulo.codeEditor.EditorSettings;
import consulo.codeEditor.LineExtensionInfo;
import consulo.colorScheme.EditorColorsScheme;
import consulo.codeEditor.EditorEx;
import consulo.language.editor.highlight.LexerEditorHighlighter;
import consulo.codeEditor.EditorHighlighter;
import consulo.codeEditor.EditorFactory;
import consulo.codeEditor.imaginary.ImaginaryEditor;
import consulo.codeEditor.TextDrawingCallback;
import consulo.colorScheme.TextAttributes;
import consulo.project.Project;
import consulo.document.util.TextRange;
import consulo.virtualFileSystem.VirtualFile;
import consulo.language.psi.PsiElement;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import org.rust.ide.highlight.RsHighlighter;
import org.rust.lang.core.macros.MacroExpansion;
import org.rust.lang.core.macros.RsExpandedElementUtil;
import org.rust.lang.core.psi.RsMacroArgument;
import org.rust.lang.core.psi.RsMacroCall;
import org.rust.lang.core.psi.ext.impl.RsPossibleMacroCallUtil;
import org.rust.lang.core.psi.ext.impl.RsPsiJavaUtil;

import java.awt.*;
import java.awt.event.KeyEvent;
// PropertyChangeListener in Consulo's EditorEx is kava.beans.PropertyChangeListener
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.function.IntFunction;
import javax.swing.JComponent;
import javax.swing.JScrollPane;
import consulo.codeEditor.IndentsModel;
import consulo.codeEditor.InlayModel;
import consulo.ui.color.ColorValue;
import consulo.ui.cursor.Cursor;

@ExtensionImpl
public class RsMacroCallSelectionHandler extends ExtendWordSelectionHandlerBase {
    @Override
    public boolean canSelect(@Nonnull PsiElement e) {
        return RsPsiJavaUtil.ancestorStrict(e, RsMacroArgument.class) != null;
    }

    @Override
    public List<TextRange> select(@Nonnull PsiElement e, @Nonnull CharSequence editorText, int cursorOffset, @Nonnull Editor editor) {
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

        FakeEditorEx(@Nonnull Project project, @Nonnull String text, @Nonnull Editor editor) {
            // DocumentImpl is platform-internal; EditorFactory is the public way to make a Document.
            super(project, EditorFactory.getInstance().createDocument(text));
            myEditor = editor;
            LexerEditorHighlighter highlighter = new LexerEditorHighlighter(new RsHighlighter(), editor.getColorsScheme());
            highlighter.setText(text);
            myHighlighter = highlighter;
        }

        @Nonnull
        @Override
        public EditorSettings getSettings() {
            return myEditor.getSettings();
        }

        @Nonnull
        @Override
        public EditorHighlighter getHighlighter() {
            return myHighlighter;
        }

        @Nonnull
        @Override
        public MarkupModelEx getMarkupModel() { throw notImplemented(); }

        @Nonnull
        @Override
        public FoldingModelEx getFoldingModel() { throw notImplemented(); }

        @Nonnull
        @Override
        public ScrollingModelEx getScrollingModel() { throw notImplemented(); }

        @Nonnull
        @Override
        public SoftWrapModelEx getSoftWrapModel() { throw notImplemented(); }

        @Nonnull
        @Override
        public MarkupModelEx getFilteredDocumentMarkupModel() { throw notImplemented(); }

        @Nonnull
        @Override
        public EditorGutterComponentEx getGutterComponentEx() { throw notImplemented(); }

        @Nonnull
        @Override
        public JComponent getPermanentHeaderComponent() { throw notImplemented(); }

        @Override
        public void setViewer(boolean isViewer) { throw notImplemented(); }

        @Override
        public void setPermanentHeaderComponent(@Nullable JComponent component) { throw notImplemented(); }

        @Override
        public void setHighlighter(@Nonnull EditorHighlighter highlighter) { throw notImplemented(); }

        @Override
        public void setColorsScheme(@Nonnull EditorColorsScheme scheme) { throw notImplemented(); }

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

        @Nonnull
        @Override
        public CutProvider getCutProvider() { throw notImplemented(); }

        @Nonnull
        @Override
        public CopyProvider getCopyProvider() { throw notImplemented(); }

        @Nonnull
        @Override
        public PasteProvider getPasteProvider() { throw notImplemented(); }

        @Nonnull
        @Override
        public DeleteProvider getDeleteProvider() { throw notImplemented(); }

        @Override
        public void repaint(int startOffset, int endOffset) { throw notImplemented(); }

        @Override
        public void reinitSettings() { throw notImplemented(); }

        public void addPropertyChangeListener(@Nonnull kava.beans.PropertyChangeListener listener, @Nonnull Disposable parentDisposable) { throw notImplemented(); }

        @Override
        public void addPropertyChangeListener(@Nonnull kava.beans.PropertyChangeListener listener) { throw notImplemented(); }

        @Override
        public int getMaxWidthInRange(int startOffset, int endOffset) { throw notImplemented(); }

        @Override
        public boolean setCaretVisible(boolean b) { throw notImplemented(); }

        @Override
        public boolean setCaretEnabled(boolean enabled) { throw notImplemented(); }

        @Override
        public void addFocusListener(@Nonnull FocusChangeListener listener) { throw notImplemented(); }

        @Override
        public void addFocusListener(@Nonnull FocusChangeListener listener, @Nonnull Disposable parentDisposable) { throw notImplemented(); }

        @Override
        public void setOneLineMode(boolean b) { throw notImplemented(); }

        @Nonnull
        @Override
        public JScrollPane getScrollPane() { throw notImplemented(); }

        @Override
        public boolean isRendererMode() { throw notImplemented(); }

        @Override
        public void setRendererMode(boolean isRendererMode) { throw notImplemented(); }

        @Override
        public void setFile(@Nonnull VirtualFile vFile) { throw notImplemented(); }

        @Nonnull
        @Override
        public DataContext getDataContext() { throw notImplemented(); }

        @Override
        public boolean processKeyTyped(@Nonnull KeyEvent e) { throw notImplemented(); }

        @Override
        public void setFontSize(int fontSize) { throw notImplemented(); }

        @Nullable
        @Override
        public consulo.ui.color.ColorValue getBackgroundColor() { throw notImplemented(); }

        @Override
        public void setBackgroundColor(@Nullable consulo.ui.color.ColorValue color) { throw notImplemented(); }

        @Nonnull
        @Override
        public Dimension getContentSize() { throw notImplemented(); }

        @Override
        public boolean isEmbeddedIntoDialogWrapper() { throw notImplemented(); }

        @Override
        public void setEmbeddedIntoDialogWrapper(boolean b) { throw notImplemented(); }

        @Nonnull
        @Override
        public VirtualFile getVirtualFile() { throw notImplemented(); }

        @Nonnull
        @Override
        public TextDrawingCallback getTextDrawingCallback() { throw notImplemented(); }

        @Nonnull
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
        public void registerLineExtensionPainter(@Nonnull IntFunction<Collection<LineExtensionInfo>> lineExtensionPainter) { throw notImplemented(); }

        public void registerScrollBarRepaintCallback(@Nullable Object callback) { throw notImplemented(); }

        @Override
        public void removePropertyChangeListener(@Nonnull kava.beans.PropertyChangeListener listener) { throw notImplemented(); }

        @Override
        public void repaint(int startOffset, int endOffset, boolean invalidateTextLayout) { throw notImplemented(); }

        @Override
        public consulo.codeEditor.InlayModel getInlayModel() { throw notImplemented(); }

        @Override
        public consulo.codeEditor.IndentsModel getIndentsModel() { throw notImplemented(); }

        @Override
        public int getExpectedCaretOffset() { throw notImplemented(); }

        @Override
        public void setContextMenuGroupId(@Nullable String groupId) { throw notImplemented(); }

        @Nullable
        @Override
        public String getContextMenuGroupId() { throw notImplemented(); }

        @Override
        public void installPopupHandler(@Nonnull EditorPopupHandler popupHandler) { throw notImplemented(); }

        @Override
        public void uninstallPopupHandler(@Nonnull EditorPopupHandler popupHandler) { throw notImplemented(); }

        @Override
        public void setCustomCursor(@Nonnull Object requestor, @Nullable Cursor cursor) { throw notImplemented(); }
    }
}
