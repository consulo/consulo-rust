/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.ide.lineMarkers;

import com.intellij.codeInsight.daemon.DaemonCodeAnalyzerSettings;
import com.intellij.codeInsight.daemon.LineMarkerInfo;
import com.intellij.codeInsight.daemon.LineMarkerProvider;
import com.intellij.openapi.editor.colors.CodeInsightColors;
import com.intellij.openapi.editor.colors.EditorColorsManager;
import com.intellij.openapi.editor.markup.SeparatorPlacement;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.psi.PsiElement;
import com.intellij.psi.util.PsiTreeUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.rust.lang.core.psi.RsFunction;
import org.rust.lang.core.psi.ext.RsElementUtil;

public class RsMethodLineSeparatorProvider implements LineMarkerProvider {

    @Nullable
    @Override
    public LineMarkerInfo<PsiElement> getLineMarkerInfo(@NotNull PsiElement element) {
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

    @NotNull
    private static LineMarkerInfo<PsiElement> createLineSeparatorByElement(@NotNull PsiElement element) {
        PsiElement anchor = PsiTreeUtil.getDeepestFirst(element);
        LineMarkerInfo<PsiElement> info = new LineMarkerInfo<>(anchor, anchor.getTextRange());
        info.separatorColor = EditorColorsManager.getInstance().getGlobalScheme().getColor(CodeInsightColors.METHOD_SEPARATORS_COLOR);
        info.separatorPlacement = SeparatorPlacement.TOP;
        return info;
    }
}
