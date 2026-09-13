/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.ide.annotator;

import consulo.language.editor.annotation.AnnotationHolder;
import consulo.language.editor.annotation.HighlightSeverity;
import consulo.language.psi.PsiElement;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import org.rust.colors.RsColor;
import org.rust.lang.core.psi.RsPatBinding;
import org.rust.lang.core.psi.RsPath;
import org.rust.lang.core.psi.RsSelfParameter;
import org.rust.lang.core.psi.RsValueParameter;
import org.rust.lang.core.psi.ext.RsElement;
import org.rust.lang.core.psi.ext.impl.RsElementUtil;
import org.rust.lang.core.psi.ext.impl.RsPatBindingUtil;
import org.rust.lang.core.psi.ext.impl.RsSelfParameterUtil;
import org.rust.lang.core.types.infer.TypeInference;
import org.rust.lang.core.types.ty.TyReference;
import org.rust.lang.core.types.ty.Ty;
import org.rust.openapiext.OpenApiUtil;
import org.rust.lang.core.types.RsTypesUtil;

public class RsHighlightingMutableAnnotator extends AnnotatorBase {

    private static final HighlightSeverity MUTABLE_HIGHLIGHTING = new HighlightSeverity(
        "MUTABLE_HIGHLIGHTING",
        HighlightSeverity.INFORMATION.myVal + 1
    );

    @Override
    protected void annotateInternal(@Nonnull PsiElement element, @Nonnull AnnotationHolder holder) {
        if (holder.isBatchMode()) return;
        RsElement ref;
        if (element instanceof RsPath) {
            PsiElement resolved = ((RsPath) element).getReference() != null ? ((RsPath) element).getReference().resolve() : null;
            if (resolved == null) return;
            ref = (RsElement) resolved;
        } else if (element instanceof RsSelfParameter) {
            ref = (RsElement) element;
        } else if (element instanceof RsPatBinding) {
            ref = (RsElement) element;
        } else {
            return;
        }
        distinctAnnotation(element, ref, holder);
    }

    @Nullable
    private RsColor annotationFor(@Nonnull RsElement ref) {
        if (ref instanceof RsSelfParameter) {
            return RsColor.MUT_PARAMETER;
        } else if (ref instanceof RsPatBinding) {
            if (RsElementUtil.ancestorStrict(ref, RsValueParameter.class) != null) {
                return RsColor.MUT_PARAMETER;
            } else {
                return RsColor.MUT_BINDING;
            }
        }
        return null;
    }

    private void distinctAnnotation(@Nonnull PsiElement element, @Nonnull RsElement ref, @Nonnull AnnotationHolder holder) {
        if (!RsElementUtil.existsAfterExpansion(element)) return;
        RsColor color = annotationFor(ref);
        if (color == null) return;
        if (isMut(ref)) {
            PsiElement target = partToHighlight(element);
            addHighlightingAnnotation(holder, target, color);
        }
    }

    @Nonnull
    private PsiElement partToHighlight(@Nonnull PsiElement element) {
        if (element instanceof RsSelfParameter) {
            return ((RsSelfParameter) element).getSelf();
        } else if (element instanceof RsPatBinding) {
            return ((RsPatBinding) element).getIdentifier();
        }
        return element;
    }

    private void addHighlightingAnnotation(@Nonnull AnnotationHolder holder, @Nonnull PsiElement target, @Nonnull RsColor key) {
        HighlightSeverity annotationSeverity = OpenApiUtil.isUnitTestMode() ? key.getTestSeverity() : MUTABLE_HIGHLIGHTING;

        holder.newSilentAnnotation(annotationSeverity)
            .range(target.getTextRange())
            .textAttributes(key.getTextAttributesKey()).create();
    }

    private static boolean isMut(@Nonnull RsElement element) {
        if (element instanceof RsPatBinding) {
            RsPatBinding binding = (RsPatBinding) element;
            if (RsPatBindingUtil.getMutability(binding).isMut()) return true;
            Ty type = RsTypesUtil.getType(binding);
            return type instanceof TyReference && ((TyReference) type).getMutability().isMut();
        } else if (element instanceof RsSelfParameter) {
            return RsSelfParameterUtil.getMutability((RsSelfParameter) element).isMut();
        }
        return false;
    }
}
