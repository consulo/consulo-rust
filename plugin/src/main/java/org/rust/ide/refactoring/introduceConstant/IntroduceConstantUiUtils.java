/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.ide.refactoring.introduceConstant;

import consulo.codeEditor.Editor;
import consulo.codeEditor.EditorColors;
import consulo.colorScheme.EditorColorsManager;
import consulo.codeEditor.markup.HighlighterLayer;
import consulo.codeEditor.markup.RangeHighlighter;
import consulo.codeEditor.markup.HighlighterTargetArea;
import consulo.codeEditor.markup.MarkupModel;
import consulo.colorScheme.TextAttributes;
import consulo.colorScheme.EffectType;
import consulo.ui.ex.popup.JBPopupFactory;
import consulo.ui.ex.popup.event.JBPopupListener;
import consulo.ui.ex.popup.event.LightweightWindowEvent;
import consulo.language.psi.PsiElement;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

import org.rust.RsBundle;
import org.rust.lang.core.psi.RsExpr;
import org.rust.lang.core.psi.RsFile;
import org.rust.lang.core.psi.RsFunction;
import org.rust.lang.core.psi.RsModItem;
import org.rust.lang.core.psi.ext.RsFunctionUtil;
import org.rust.openapiext.OpenApiUtil;

import javax.swing.*;
import java.awt.*;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

public final class IntroduceConstantUiUtils {

    private IntroduceConstantUiUtils() {
    }

    @Nullable
    public static ExtractConstantUi MOCK = null;

    public static void showInsertionChooser(
        @Nonnull Editor editor,
        @Nonnull RsExpr expr,
        @Nonnull Consumer<InsertionCandidate> callback
    ) {
        List<InsertionCandidate> candidates = findInsertionCandidates(expr);
        if (org.rust.openapiext.OpenApiUtil.isUnitTestMode()) {
            callback.accept(MOCK.chooseInsertionPoint(expr, candidates));
        } else {
            Highlighter highlighter = new Highlighter(editor);
            JBPopupFactory.getInstance()
                .createPopupChooserBuilder(candidates)
                .setRenderer(new DefaultListCellRenderer() {
                    @Override
                    public Component getListCellRendererComponent(
                        JList<?> list, Object value, int index, boolean isSelected, boolean cellHasFocus
                    ) {
                        InsertionCandidate candidate = (InsertionCandidate) value;
                        String text = candidate.description();
                        return super.getListCellRendererComponent(list, text, index, isSelected, cellHasFocus);
                    }
                })
                .setItemSelectedCallback(value -> {
                    if (value == null) return;
                    highlighter.onSelect(value);
                })
                .setTitle(RsBundle.message("popup.title.choose.scope.to.introduce.constant", expr.getText()))
                .setMovable(true)
                .setResizable(false)
                .setRequestFocus(true)
                .setItemChosenCallback(chosen -> {
                    if (chosen != null) callback.accept(chosen);
                })
                .addListener(highlighter)
                .createPopup()
                .showInBestPositionFor(consulo.dataContext.DataManager.getInstance().getDataContext(editor.getContentComponent()));
        }
    }

    @Nonnull
    public static List<InsertionCandidate> findInsertionCandidates(@Nonnull RsExpr expr) {
        PsiElement parent = expr;
        PsiElement anchor = expr;
        List<InsertionCandidate> points = new ArrayList<>();

        boolean moduleVisited = false;
        while (!(parent instanceof RsFile)) {
            parent = parent.getParent();
            if (parent instanceof RsFunction) {
                if (!moduleVisited) {
                    RsFunction function = (RsFunction) parent;
                    PsiElement block = RsFunctionUtil.getBlock(function);
                    if (block != null) {
                        points.add(new InsertionCandidate(parent, block, getAnchor(block, anchor)));
                        anchor = parent;
                    }
                }
            } else if (parent instanceof RsModItem || parent instanceof RsFile) {
                points.add(new InsertionCandidate(parent, parent, getAnchor(parent, anchor)));
                anchor = parent;
                moduleVisited = true;
            }
        }
        return points;
    }

    @Nonnull
    private static PsiElement getAnchor(@Nonnull PsiElement parent, @Nonnull PsiElement anchor) {
        PsiElement found = anchor;
        while (found.getParent() != parent) {
            found = found.getParent();
        }
        return found;
    }

    
    public static void withMockExtractConstantChooser(@Nonnull ExtractConstantUi mock, @Nonnull Runnable f) {
        MOCK = mock;
        try {
            f.run();
        } finally {
            MOCK = null;
        }
    }

    public static class Highlighter implements JBPopupListener {
        @Nullable
        private RangeHighlighter highlighter;
        @Nonnull
        private final Editor editor;
        @Nonnull
        private final TextAttributes attributes;

        public Highlighter(@Nonnull Editor editor) {
            this.editor = editor;
            this.attributes = EditorColorsManager.getInstance().getGlobalScheme()
                .getAttributes(EditorColors.SEARCH_RESULT_ATTRIBUTES);
        }

        public void onSelect(@Nonnull InsertionCandidate candidate) {
            dropHighlighter();
            MarkupModel markupModel = editor.getMarkupModel();
            consulo.document.util.TextRange textRange = candidate.getParent().getTextRange();
            highlighter = markupModel.addRangeHighlighter(
                textRange.getStartOffset(), textRange.getEndOffset(),
                HighlighterLayer.SELECTION - 1, attributes,
                HighlighterTargetArea.EXACT_RANGE
            );
        }

        @Override
        public void onClosed(@Nonnull LightweightWindowEvent event) {
            dropHighlighter();
        }

        private void dropHighlighter() {
            if (highlighter != null) {
                highlighter.dispose();
                highlighter = null;
            }
        }
    }
}
