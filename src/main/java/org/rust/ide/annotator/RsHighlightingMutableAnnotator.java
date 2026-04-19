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
import org.rust.lang.core.psi.RsPatBinding;
import org.rust.lang.core.psi.RsPath;
import org.rust.lang.core.psi.RsSelfParameter;
import org.rust.lang.core.psi.RsValueParameter;
import org.rust.lang.core.psi.ext.RsElement;
import org.rust.lang.core.psi.ext.RsElementUtil;
import org.rust.lang.core.psi.ext.RsPatBindingUtil;
import org.rust.lang.core.psi.ext.RsSelfParameterUtil;
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
    protected void annotateInternal(@NotNull PsiElement element, @NotNull AnnotationHolder holder) {
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
    private RsColor annotationFor(@NotNull RsElement ref) {
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

    private void distinctAnnotation(@NotNull PsiElement element, @NotNull RsElement ref, @NotNull AnnotationHolder holder) {
        if (!RsElementUtil.existsAfterExpansion(element)) return;
        RsColor color = annotationFor(ref);
        if (color == null) return;
        if (isMut(ref)) {
            PsiElement target = partToHighlight(element);
            addHighlightingAnnotation(holder, target, color);
        }
    }

    @NotNull
    private PsiElement partToHighlight(@NotNull PsiElement element) {
        if (element instanceof RsSelfParameter) {
            return ((RsSelfParameter) element).getSelf();
        } else if (element instanceof RsPatBinding) {
            return ((RsPatBinding) element).getIdentifier();
        }
        return element;
    }

    private void addHighlightingAnnotation(@NotNull AnnotationHolder holder, @NotNull PsiElement target, @NotNull RsColor key) {
        HighlightSeverity annotationSeverity = OpenApiUtil.isUnitTestMode() ? key.getTestSeverity() : MUTABLE_HIGHLIGHTING;

        holder.newSilentAnnotation(annotationSeverity)
            .range(target.getTextRange())
            .textAttributes(key.getTextAttributesKey()).create();
    }

    private static boolean isMut(@NotNull RsElement element) {
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
