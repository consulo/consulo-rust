/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.ide.lineMarkers;

import consulo.language.editor.DaemonCodeAnalyzerSettings;
import consulo.language.editor.gutter.LineMarkerInfo;
import consulo.language.editor.gutter.LineMarkerProvider;
import consulo.codeEditor.CodeInsightColors;
import consulo.colorScheme.EditorColorsManager;
import consulo.codeEditor.markup.SeparatorPlacement;
import consulo.util.lang.StringUtil;
import consulo.language.psi.PsiElement;
import consulo.language.psi.util.PsiTreeUtil;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import org.rust.lang.core.psi.RsFunction;
import org.rust.lang.core.psi.ext.RsElementUtil;
import consulo.annotation.component.ExtensionImpl;
import consulo.codeEditor.markup.GutterIconRenderer;
import consulo.language.Language;
import consulo.language.editor.Pass;
import org.rust.lang.RsLanguage;

@ExtensionImpl
public class RsMethodLineSeparatorProvider implements LineMarkerProvider {

    @Nonnull
    @Override
    public consulo.language.Language getLanguage() {
        return org.rust.lang.RsLanguage.INSTANCE;
    }

    @Nullable
    @Override
    public LineMarkerInfo<PsiElement> getLineMarkerInfo(@Nonnull PsiElement element) {
        if (DaemonCodeAnalyzerSettings.getInstance().SHOW_METHOD_SEPARATORS) {
            if (canHaveSeparator(element)) {
                PsiElement prevSibling = RsElementUtil.getPrevNonCommentSibling(element);
                if (canHaveSeparator(prevSibling) && (wantsSeparator(element) || wantsSeparator(prevSibling))) {
                    return createLineSeparatorByElement(element);
                }
            }
        }
        return null;
    }

    private static boolean canHaveSeparator(@Nullable PsiElement element) {
        return element instanceof RsFunction;
    }

    private static boolean wantsSeparator(@Nullable PsiElement element) {
        if (element == null) return false;
        return StringUtil.getLineBreakCount(element.getText()) > 0;
    }

    @Nonnull
    private static LineMarkerInfo<PsiElement> createLineSeparatorByElement(@Nonnull PsiElement element) {
        PsiElement anchor = PsiTreeUtil.getDeepestFirst(element);
        LineMarkerInfo<PsiElement> info = new LineMarkerInfo<PsiElement>(
            anchor, anchor.getTextRange(), null, consulo.language.editor.Pass.LINE_MARKERS,
            null, null, consulo.codeEditor.markup.GutterIconRenderer.Alignment.RIGHT);
        info.separatorColor = EditorColorsManager.getInstance().getGlobalScheme().getColor(CodeInsightColors.METHOD_SEPARATORS_COLOR);
        info.separatorPlacement = SeparatorPlacement.TOP;
        return info;
    }
}
