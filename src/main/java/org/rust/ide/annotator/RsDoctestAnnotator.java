/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.ide.annotator;

import org.rust.lang.core.psi.ext.RsElementUtil;
import com.intellij.lang.annotation.AnnotationHolder;
import com.intellij.lang.annotation.HighlightSeverity;
import com.intellij.openapi.editor.colors.EditorColors;
import com.intellij.openapi.util.TextRange;
import com.intellij.psi.PsiElement;
import org.jetbrains.annotations.NotNull;
import org.rust.ide.injected.DoctestInfoUtil;
import org.rust.ide.injected.DoctestInfo;
import org.rust.lang.core.psi.ext.RsElement;
import org.rust.lang.doc.psi.RsDocCodeFence;

import java.util.List;

/**
 * Adds missing background for injections from RsDoctestLanguageInjector.
 * Background is disabled by InjectionBackgroundSuppressor marker implemented for RsDocCodeFence.
 *
 * We have to do it this way because we want to highlight fully range inside backticks
 * but a real injection is shifted by 1 character and empty lines are skipped.
 */
public class RsDoctestAnnotator extends AnnotatorBase {
    @Override
    protected void annotateInternal(@NotNull PsiElement element, @NotNull AnnotationHolder holder) {
        if (holder.isBatchMode()) return;
        if (!(element instanceof RsDocCodeFence)) return;
        RsDocCodeFence codeFence = (RsDocCodeFence) element;
        DoctestInfo doctest = DoctestInfoUtil.doctestInfo(codeFence);
        if (doctest == null) return;

        int startOffset = RsElementUtil.getStartOffset(element);
        List<TextRange> ranges = doctest.getRangesForBackgroundHighlighting();
        for (TextRange range : ranges) {
            holder.newSilentAnnotation(HighlightSeverity.INFORMATION)
                .range(range.shiftRight(startOffset))
                .textAttributes(EditorColors.INJECTED_LANGUAGE_FRAGMENT)
                .create();
        }
    }
}
