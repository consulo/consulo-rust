/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.ide.annotator;

import consulo.language.editor.annotation.AnnotationHolder;
import consulo.language.editor.annotation.HighlightSeverity;
import consulo.language.psi.PsiElement;
import jakarta.annotation.Nonnull;
import org.rust.RsBundle;
import org.rust.colors.RsColor;
import org.rust.lang.core.crate.Crate;
import org.rust.lang.core.psi.ext.RsAttr;
import org.rust.lang.core.psi.ext.impl.RsAttrUtil;
import org.rust.lang.core.psi.ext.RsDocAndAttributeOwner;
import org.rust.lang.core.psi.ext.impl.RsDocAndAttributeOwnerUtil;
import org.rust.openapiext.OpenApiUtil;
import org.rust.lang.core.psi.ext.impl.RsAttrExtUtil;

public class RsCfgDisabledCodeAnnotator extends AnnotatorBase {

    public static final HighlightSeverity CONDITIONALLY_DISABLED_CODE_SEVERITY = new HighlightSeverity(
        "CONDITIONALLY_DISABLED_CODE",
        HighlightSeverity.INFORMATION.myVal + 1
    );

    @Override
    protected void annotateInternal(@Nonnull PsiElement element, @Nonnull AnnotationHolder holder) {
        if (holder.isBatchMode()) return;

        if (shouldHighlightAsCfsDisabled(element, holder)) {
            createCondDisabledAnnotation(holder);
        }
    }

    private void createCondDisabledAnnotation(@Nonnull AnnotationHolder holder) {
        RsColor color = RsColor.CFG_DISABLED_CODE;
        HighlightSeverity severity = OpenApiUtil.isUnitTestMode() ? color.getTestSeverity() : CONDITIONALLY_DISABLED_CODE_SEVERITY;

        holder.newAnnotation(severity, RsBundle.message("text.conditionally.disabled.code"))
            .textAttributes(color.getTextAttributesKey())
            .create();
    }

    public static boolean shouldHighlightAsCfsDisabled(@Nonnull PsiElement element, @Nonnull AnnotationHolder holder) {
        Crate crate = AnnotationSessionEx.currentCrate(holder);
        if (crate == null) return false;

        if (element instanceof RsDocAndAttributeOwner && !RsDocAndAttributeOwnerUtil.isEnabledByCfgSelfOrInAttrProcMacroBody((RsDocAndAttributeOwner) element, crate)) {
            return true;
        }

        if (element instanceof RsAttr) {
            RsAttr attr = (RsAttr) element;
            if (RsAttrExtUtil.isDisabledCfgAttrAttribute(attr, crate)) {
                RsDocAndAttributeOwner owner = RsAttrUtil.getOwner(attr);
                if (owner != null && RsDocAndAttributeOwnerUtil.isEnabledByCfgSelfOrInAttrProcMacroBody(owner, crate)) {
                    return true;
                }
            }
        }

        return false;
    }
}
