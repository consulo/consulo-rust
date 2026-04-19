/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.ide.annotator;

import com.intellij.lang.annotation.AnnotationHolder;
import com.intellij.lang.annotation.HighlightSeverity;
import com.intellij.psi.PsiElement;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.rust.ide.colors.RsColor;
import org.rust.ide.injected.DoctestInfoUtil;
import org.rust.lang.core.psi.ext.RsElementUtil;
import org.rust.lang.core.psi.ext.RsPsiElementExt;
import org.rust.lang.doc.psi.*;
import org.rust.openapiext.OpenApiUtil;
import org.rust.lang.core.psi.ext.RsPsiJavaUtil;

public class RsDocHighlightingAnnotator extends AnnotatorBase {
    @Override
    protected void annotateInternal(@NotNull PsiElement element, @NotNull AnnotationHolder holder) {
        if (holder.isBatchMode()) return;
        RsColor color = getColor(element);
        if (color == null) return;

        if (!RsPsiElementExt.isEnabledByCfg(element)) return;

        HighlightSeverity severity = OpenApiUtil.isUnitTestMode() ? color.getTestSeverity() : HighlightSeverity.INFORMATION;

        holder.newSilentAnnotation(severity).textAttributes(color.getTextAttributesKey()).create();
    }

    @Nullable
    private RsColor getColor(@NotNull PsiElement element) {
        if (RsElementUtil.getElementType(element) == RsDocElementTypes.DOC_DATA) {
            PsiElement parent = element.getParent();
            if (parent instanceof RsDocCodeFence) {
                if (isDoctestInjected((RsDocCodeFence) parent)) {
                    return null;
                } else {
                    return RsColor.DOC_CODE;
                }
            } else if (parent instanceof RsDocCodeFenceStartEnd || parent instanceof RsDocCodeFenceLang) {
                return RsColor.DOC_CODE;
            } else if (parent instanceof RsDocCodeSpan) {
                if (RsElementUtil.ancestorStrict(element, RsDocLink.class) == null) {
                    return RsColor.DOC_CODE;
                } else {
                    return null;
                }
            } else if (parent instanceof RsDocCodeBlock) {
                return RsColor.DOC_CODE;
            } else {
                return null;
            }
        } else if (element instanceof RsDocEmphasis) {
            return RsColor.DOC_EMPHASIS;
        } else if (element instanceof RsDocStrong) {
            return RsColor.DOC_STRONG;
        } else if (element instanceof RsDocAtxHeading) {
            return RsColor.DOC_HEADING;
        } else if (element instanceof RsDocLink && RsPsiJavaUtil.descendantOfTypeStrict(element, RsDocGap.class) == null) {
            return RsColor.DOC_LINK;
        } else {
            return null;
        }
    }

    private boolean isDoctestInjected(@NotNull RsDocCodeFence codeFence) {
        return DoctestInfoUtil.doctestInfo(codeFence) != null;
    }
}
