/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.ide.fixes;

import com.intellij.codeInsight.intention.FileModifier.SafeFieldForPreview;
import org.jetbrains.annotations.NotNull;
import org.rust.lang.core.psi.*;
import org.rust.lang.core.psi.ext.RsFunctionOrLambda;
import org.rust.lang.core.psi.ext.RsTraitRefUtil;
import org.rust.lang.core.resolve.KnownItems;
import org.rust.lang.core.types.*;
import org.rust.lang.core.types.ty.Ty;
import org.rust.lang.core.types.ty.TyAdt;
import org.rust.lang.core.types.RsTypesUtil;

/**
 * Similar to ConvertToTyUsingTryTraitFix, but also "unwraps" the result with unwrap() or ?.
 */
public abstract class ConvertToTyUsingTryTraitAndUnpackFix extends ConvertToTyUsingTryTraitFix {

    @SafeFieldForPreview
    private final Ty errTy;

    public ConvertToTyUsingTryTraitAndUnpackFix(
        @NotNull RsExpr expr,
        @NotNull Ty ty,
        @NotNull Ty errTy,
        @NotNull String traitName,
        @NotNull FromCallMaker fromCallMaker
    ) {
        super(expr, ty, traitName, fromCallMaker);
        this.errTy = errTy;
    }

    @Override
    protected void addFromCall(@NotNull RsPsiFactory psiFactory, @NotNull RsExpr expr, @NotNull RsExpr fromCall) {
        Ty parentFnRetTy = findParentFnOrLambdaRetTy(expr);
        if (parentFnRetTy != null && isFnRetTyResultAndMatchErrTy(expr, parentFnRetTy)) {
            expr.replace(psiFactory.createTryExpression(fromCall));
        } else {
            expr.replace(psiFactory.createNoArgsMethodCall(fromCall, "unwrap"));
        }
    }

    private Ty findParentFnOrLambdaRetTy(@NotNull RsExpr element) {
        RsRetType retType = findParentFunctionOrLambdaRsRetType(element);
        if (retType == null) return null;
        RsTypeReference typeRef = retType.getTypeReference();
        if (typeRef == null) return null;
        return RsTypesUtil.getNormType(typeRef);
    }

    private RsRetType findParentFunctionOrLambdaRsRetType(@NotNull RsExpr element) {
        com.intellij.psi.PsiElement parent = element.getParent();
        while (parent != null) {
            if (parent instanceof RsFunctionOrLambda) {
                return ((RsFunctionOrLambda) parent).getRetType();
            }
            parent = parent.getParent();
        }
        return null;
    }

    private boolean isFnRetTyResultAndMatchErrTy(@NotNull RsExpr element, @NotNull Ty fnRetTy) {
        var lookup = org.rust.lang.core.types.RsTypesUtil.getImplLookup(element);
        var items = org.rust.lang.core.resolve.KnownItems.getKnownItems(element);
        if (!(fnRetTy instanceof TyAdt)) return false;
        TyAdt adtRetTy = (TyAdt) fnRetTy;
        if (adtRetTy.getItem() != items.getResult()) return false;
        var fromTrait = items.getFrom();
        if (fromTrait == null) return false;
        Ty errArgTy = adtRetTy.getTypeArguments().get(1);
        TraitRef traitRef = new TraitRef(errArgTy, org.rust.lang.core.psi.ext.RsGenericDeclarationUtil.withSubst(fromTrait, this.errTy));
        return lookup.select(traitRef).ok() != null;
    }
}
