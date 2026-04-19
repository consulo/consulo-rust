/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.ide.hints.value;

import com.intellij.codeInsight.hints.*;
import com.intellij.codeInsight.hints.ImmediateConfigurable.Case;
import com.intellij.codeInsight.hints.presentation.InlayPresentation;
import com.intellij.codeInsight.hints.presentation.InsetPresentation;
import com.intellij.codeInsight.hints.presentation.MenuOnClickPresentation;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.project.DumbService;
import com.intellij.openapi.project.Project;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.rust.RsBundle;
import org.rust.lang.RsLanguage;
import org.rust.lang.core.psi.RsElementTypes;
import org.rust.lang.core.psi.RsMacroCall;
import org.rust.lang.core.psi.RsPatRange;
import org.rust.lang.core.psi.RsRangeExpr;
import org.rust.lang.core.psi.ext.RsElement;
import org.rust.lang.core.psi.ext.RsElementUtil;

import javax.swing.JComponent;
import javax.swing.JPanel;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@SuppressWarnings("UnstableApiUsage")
public class RsInlayValueHintsProvider implements InlayHintsProvider<RsInlayValueHintsProvider.Settings> {

    private static final SettingsKey<Settings> KEY = new SettingsKey<>("rust.value.range.exclusive.hints");

    @NotNull
    @Override
    public SettingsKey<Settings> getKey() {
        return KEY;
    }

    @NotNull
    @Override
    public String getName() {
        return RsBundle.message("settings.rust.inlay.hints.title.values");
    }

    @Nullable
    @Override
    public String getPreviewText() {
        return null;
    }

    @NotNull
    @Override
    public InlayGroup getGroup() {
        return InlayGroup.VALUES_GROUP;
    }

    @NotNull
    @Override
    public ImmediateConfigurable createConfigurable(@NotNull Settings settings) {
        return new ImmediateConfigurable() {
            @NotNull
            @Override
            public String getMainCheckboxText() {
                return RsBundle.message("settings.rust.inlay.hints.for");
            }

            @NotNull
            @Override
            public List<Case> getCases() {
                // which cannot be properly implemented in Java.
                return Collections.emptyList();
            }

            @NotNull
            @Override
            public JComponent createComponent(@NotNull ChangeListener listener) {
                return new JPanel();
            }
        };
    }

    @NotNull
    @Override
    public Settings createSettings() {
        return new Settings();
    }

    @NotNull
    @Override
    public InlayHintsCollector getCollectorFor(@NotNull PsiFile file, @NotNull Editor editor, @NotNull Settings settings, @NotNull InlayHintsSink sink) {
        Project project = file.getProject();

        return new FactoryInlayHintsCollector(editor) {
            @Override
            public boolean collect(@NotNull PsiElement element, @NotNull Editor editor, @NotNull InlayHintsSink sink) {
                if (DumbService.isDumb(project)) return true;
                if (!(element instanceof RsElement)) return true;

                if (element instanceof RsMacroCall) {
                    return false;
                }
                if (settings.myShowForExpressions) {
                    presentExpression((RsElement) element, project, sink);
                }
                if (settings.myShowForPatterns) {
                    presentPattern((RsElement) element, project, sink);
                }

                return true;
            }

            private void presentExpression(@NotNull RsElement expr, @NotNull Project project, @NotNull InlayHintsSink sink) {
                if (!(expr instanceof RsRangeExpr)) return;
                RsRangeExpr rangeExpr = (RsRangeExpr) expr;
                if (!RsElementUtil.isEnabledByCfg(rangeExpr)) return;
                PsiElement end = org.rust.lang.core.psi.ext.RsRangeExprUtil.getEnd(rangeExpr);
                PsiElement op = org.rust.lang.core.psi.ext.RsRangeExprUtil.getOp(rangeExpr);
                if (end == null || op == null) return;
                PresentationInfo info = getPresentationInfo(end.getTextRange().getStartOffset(), op, project);
                if (info == null) return;
                sink.addInlineElement(info.myOffset, false, info.myPresentation, false);
            }

            private void presentPattern(@NotNull RsElement pat, @NotNull Project project, @NotNull InlayHintsSink sink) {
                if (!(pat instanceof RsPatRange)) return;
                RsPatRange patRange = (RsPatRange) pat;
                if (!RsElementUtil.isEnabledByCfg(patRange)) return;
                PsiElement end = org.rust.lang.core.psi.ext.RsPatRangeUtil.getEnd(patRange);
                PsiElement op = org.rust.lang.core.psi.ext.RsPatRangeUtil.getOp(patRange);
                if (end == null || op == null) return;
                PresentationInfo info = getPresentationInfo(end.getTextRange().getStartOffset(), op, project);
                if (info == null) return;
                sink.addInlineElement(info.myOffset, false, info.myPresentation, false);
            }

            @Nullable
            private PresentationInfo getPresentationInfo(@Nullable Integer offsetEnd, @Nullable PsiElement range, @NotNull Project project) {
                if (range == null || offsetEnd == null) return null;
                String textEnd = null;
                if (RsElementUtil.getElementType(range) == RsElementTypes.DOTDOT) {
                    textEnd = "<";
                }
                if (textEnd == null) return null;
                InlayPresentation presentation = withDisableAction(getFactory().roundWithBackground(getFactory().text(textEnd)), project);
                return new PresentationInfo(presentation, offsetEnd);
            }
        };
    }

    @NotNull
    private InlayPresentation withDisableAction(@NotNull InlayPresentation presentation, @NotNull Project project) {
        return new InsetPresentation(
            new MenuOnClickPresentation(presentation, project, () ->
                Collections.singletonList(new InlayProviderDisablingAction(getName(), RsLanguage.INSTANCE, project, KEY))
            ), 1, 0, 0, 0
        );
    }

    public static class Settings {
        private boolean myShowForExpressions = true;
        private boolean myShowForPatterns = true;

        public boolean getShowForExpressions() { return myShowForExpressions; }
        public void setShowForExpressions(boolean value) { myShowForExpressions = value; }

        public boolean getShowForPatterns() { return myShowForPatterns; }
        public void setShowForPatterns(boolean value) { myShowForPatterns = value; }
    }

    private static class PresentationInfo {
        private final InlayPresentation myPresentation;
        private final int myOffset;

        PresentationInfo(@NotNull InlayPresentation presentation, int offset) {
            myPresentation = presentation;
            myOffset = offset;
        }
    }
}
