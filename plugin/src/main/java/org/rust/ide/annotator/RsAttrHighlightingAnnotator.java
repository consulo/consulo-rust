/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.ide.annotator;

import consulo.language.editor.annotation.AnnotationHolder;
import consulo.language.editor.annotation.HighlightSeverity;
import consulo.language.psi.PsiElement;
import jakarta.annotation.Nonnull;
import org.rust.ide.colors.RsColor;
import org.rust.lang.core.psi.ext.RsAttr;
import org.rust.lang.core.psi.ext.RsElementUtil;
import org.rust.openapiext.OpenApiUtil;

public class RsAttrHighlightingAnnotator extends AnnotatorBase {
    @Override
    protected void annotateInternal(@Nonnull PsiElement element, @Nonnull AnnotationHolder holder) {
        if (holder.isBatchMode()) return;
        RsColor color;
        if (element instanceof RsAttr) {
            color = RsColor.ATTRIBUTE;
        } else {
            return;
        }

        if (!RsElementUtil.existsAfterExpansion(element)) return;

        HighlightSeverity severity = OpenApiUtil.isUnitTestMode() ? color.getTestSeverity() : HighlightSeverity.INFORMATION;

        holder.newSilentAnnotation(severity).textAttributes(color.getTextAttributesKey()).create();
    }
}
