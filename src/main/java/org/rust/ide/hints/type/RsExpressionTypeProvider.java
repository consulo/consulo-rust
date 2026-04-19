/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.ide.hints.type;

import com.intellij.lang.ExpressionTypeProvider;
import com.intellij.openapi.util.TextRange;
import com.intellij.psi.PsiElement;
import com.intellij.psi.impl.FakePsiElement;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.rust.RsBundle;
import org.rust.ide.presentation.TypeRendering;
import org.rust.lang.core.macros.MacroExpansionExtUtil;
import org.rust.lang.core.psi.RsExpr;
import org.rust.lang.core.psi.RsPat;
import org.rust.lang.core.psi.RsPatField;
import org.rust.lang.core.psi.RsStructLiteralField;
import org.rust.lang.core.psi.ext.RsElementUtil;
import org.rust.lang.core.psi.ext.RsItemElement;
import org.rust.lang.core.types.RsTypesUtil;
import org.rust.lang.core.types.infer.Adjustment;
import org.rust.lang.core.types.ty.Ty;
import org.rust.openapiext.OpenApiUtil;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import org.rust.lang.core.types.ExtensionsUtil;

public class RsExpressionTypeProvider extends ExpressionTypeProvider<PsiElement> {

    @NotNull
    @Override
    public String getErrorHint() {
        return RsBundle.message("hint.text.select.expression");
    }

    @NotNull
    @Override
    public List<PsiElement> getExpressionsAt(@NotNull PsiElement pivot) {
        PsiElement expanded = MacroExpansionExtUtil.findExpansionElementOrSelf(pivot);
        List<PsiElement> result = new ArrayList<>();
        PsiElement current = expanded;
        while (current != null && !(current instanceof RsItemElement)) {
            if (current instanceof RsExpr || current instanceof RsPat || current instanceof RsPatField || current instanceof RsStructLiteralField) {
                PsiElement wrapped = wrapExpandedElements(current);
                if (wrapped != null) {
                    result.add(wrapped);
                }
            }
            current = current.getParent();
        }
        return result;
    }

    @NotNull
    @Override
    public String getInformationHint(@NotNull PsiElement element) {
        TypeAndAdjusted typeAndAdjusted = getType(element);
        Ty type = typeAndAdjusted.myType;
        AdjustedType adjustedType = typeAndAdjusted.myAdjustedType;
        if (adjustedType != null) {
            String renderedType = renderTy(type);
            String renderedAdjusted = renderTy(adjustedType.myFinalTy);
            String alignedType = OpenApiUtil.escaped(renderedType);
            String alignedCoerced = OpenApiUtil.escaped(renderedAdjusted);
            return RsBundle.message("hint.text.html.table.tr.td.style.color.type.td.td.style.font.family.monospace.td.tr.tr.td.style.color.coerced.type.td.td.style.font.family.monospace.td.tr.table.html", alignedType, alignedCoerced).trim();
        } else {
            return OpenApiUtil.escaped(renderTy(type));
        }
    }

    @NotNull
    private TypeAndAdjusted getType(@NotNull PsiElement element) {
        TypeAndAdjustments typeAndAdj = getTypeAndAdjustments(element);
        Ty type = typeAndAdj.myType;
        List<Adjustment> adjustments = typeAndAdj.myAdjustments;

        Ty derefTy = type;
        for (Adjustment adj : adjustments) {
            if (adj instanceof Adjustment.Deref) {
                derefTy = adj.getTarget();
            } else {
                break;
            }
        }

        Ty finalTy = adjustments.isEmpty() ? null : adjustments.get(adjustments.size() - 1).getTarget();
        if (finalTy == null) {
            return new TypeAndAdjusted(type, null);
        } else {
            return new TypeAndAdjusted(type, new AdjustedType(derefTy, finalTy));
        }
    }

    @NotNull
    private TypeAndAdjustments getTypeAndAdjustments(@NotNull PsiElement element) {
        if (element instanceof MyFakePsiElement) {
            return getTypeAndAdjustments(((MyFakePsiElement) element).myElementInMacroExpansion);
        }
        if (element instanceof RsExpr) {
            return new TypeAndAdjustments(RsTypesUtil.getType((RsExpr) element), ExtensionsUtil.getAdjustments((RsExpr) element));
        }
        if (element instanceof RsPat) {
            return new TypeAndAdjustments(RsTypesUtil.getType((RsPat) element), Collections.emptyList());
        }
        if (element instanceof RsPatField) {
            return new TypeAndAdjustments(RsTypesUtil.getType((RsPatField) element), Collections.emptyList());
        }
        if (element instanceof RsStructLiteralField) {
            return new TypeAndAdjustments(RsTypesUtil.getType((RsStructLiteralField) element), ExtensionsUtil.getAdjustments((RsStructLiteralField) element));
        }
        throw new IllegalStateException("Unexpected element type: " + element);
    }

    @NotNull
    private String renderTy(@NotNull Ty ty) {
        return TypeRendering.render(ty, null, Integer.MAX_VALUE, "<unknown>", "<anonymous>", "'<unknown>", "<unknown>", "{integer}", "{float}", Collections.emptySet(), true, false, true, false);
    }

    @Nullable
    private static PsiElement wrapExpandedElements(@NotNull PsiElement element) {
        PsiElement macroCall = MacroExpansionExtUtil.findMacroCallExpandedFromNonRecursive(element);
        if (macroCall != null) {
            TextRange rangeInExpansion = element.getTextRange();
            TextRange rangeInMacroCall = MacroExpansionExtUtil.mapRangeFromExpansionToCallBodyStrict(macroCall, rangeInExpansion);
            if (rangeInMacroCall == null) return null;
            return new MyFakePsiElement(element, rangeInMacroCall);
        } else {
            return element;
        }
    }

    private static class TypeAndAdjusted {
        private final Ty myType;
        @Nullable
        private final AdjustedType myAdjustedType;

        TypeAndAdjusted(@NotNull Ty type, @Nullable AdjustedType adjustedType) {
            myType = type;
            myAdjustedType = adjustedType;
        }
    }

    private static class AdjustedType {
        private final Ty myDerefTy;
        private final Ty myFinalTy;

        AdjustedType(@NotNull Ty derefTy, @NotNull Ty finalTy) {
            myDerefTy = derefTy;
            myFinalTy = finalTy;
        }
    }

    private static class TypeAndAdjustments {
        private final Ty myType;
        private final List<Adjustment> myAdjustments;

        TypeAndAdjustments(@NotNull Ty type, @NotNull List<Adjustment> adjustments) {
            myType = type;
            myAdjustments = adjustments;
        }
    }

    private static class MyFakePsiElement extends FakePsiElement {
        private final PsiElement myElementInMacroExpansion;
        private final TextRange myTextRangeInMacroCall;

        MyFakePsiElement(@NotNull PsiElement elementInMacroExpansion, @NotNull TextRange textRangeInMacroCall) {
            myElementInMacroExpansion = elementInMacroExpansion;
            myTextRangeInMacroCall = textRangeInMacroCall;
        }

        @Override
        public PsiElement getParent() {
            return myElementInMacroExpansion.getParent();
        }

        @Override
        public TextRange getTextRange() {
            return myTextRangeInMacroCall;
        }

        @Override
        public String getText() {
            return myElementInMacroExpansion.getText();
        }
    }
}
