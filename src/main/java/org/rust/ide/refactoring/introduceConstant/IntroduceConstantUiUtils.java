/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.ide.refactoring.introduceConstant;

import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.editor.colors.EditorColors;
import com.intellij.openapi.editor.colors.EditorColorsManager;
import com.intellij.openapi.editor.markup.*;
import com.intellij.openapi.ui.popup.JBPopupFactory;
import com.intellij.openapi.ui.popup.JBPopupListener;
import com.intellij.openapi.ui.popup.LightweightWindowEvent;
import com.intellij.psi.PsiElement;
import org.jetbrains.annotations.Nls;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.TestOnly;
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
        @NotNull Editor editor,
        @NotNull RsExpr expr,
        @NotNull Consumer<InsertionCandidate> callback
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
                .showInBestPositionFor(editor);
        }
    }

    @NotNull
    public static List<InsertionCandidate> findInsertionCandidates(@NotNull RsExpr expr) {
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

    @NotNull
    private static PsiElement getAnchor(@NotNull PsiElement parent, @NotNull PsiElement anchor) {
        PsiElement found = anchor;
        while (found.getParent() != parent) {
            found = found.getParent();
        }
        return found;
    }

    @TestOnly
    public static void withMockExtractConstantChooser(@NotNull ExtractConstantUi mock, @NotNull Runnable f) {
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
        @NotNull
        private final Editor editor;
        @NotNull
        private final TextAttributes attributes;

        public Highlighter(@NotNull Editor editor) {
            this.editor = editor;
            this.attributes = EditorColorsManager.getInstance().getGlobalScheme()
                .getAttributes(EditorColors.SEARCH_RESULT_ATTRIBUTES);
        }

        public void onSelect(@NotNull InsertionCandidate candidate) {
            dropHighlighter();
            MarkupModel markupModel = editor.getMarkupModel();
            com.intellij.openapi.util.TextRange textRange = candidate.getParent().getTextRange();
            highlighter = markupModel.addRangeHighlighter(
                textRange.getStartOffset(), textRange.getEndOffset(),
                HighlighterLayer.SELECTION - 1, attributes,
                HighlighterTargetArea.EXACT_RANGE
            );
        }

        @Override
        public void onClosed(@NotNull LightweightWindowEvent event) {
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
