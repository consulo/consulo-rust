/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.ide.presentation;

import org.jetbrains.annotations.NotNull;
import org.rust.lang.core.psi.RsLifetime;
import org.rust.lang.core.psi.RsTypeReference;
import org.rust.lang.core.psi.ext.RsElement;
import org.rust.lang.core.types.Substitution;
import org.rust.lang.core.types.ty.Ty;
import org.rust.lang.core.types.consts.CtConstParameter;
import org.rust.lang.core.types.consts.CtValue;
import org.rust.lang.core.types.LifetimeUtil;
import org.rust.lang.core.types.SubstitutionUtil;
import org.rust.lang.core.types.regions.ReEarlyBound;
import org.rust.lang.core.types.regions.Region;
import org.rust.lang.core.types.ty.TyTypeParameter;
import org.rust.lang.core.psi.RsExpr;
import org.rust.lang.core.psi.RsTypeArgumentList;
import org.rust.lang.core.psi.ext.RsStubbedElementUtil;
import org.rust.lang.utils.evaluation.ConstExpr;
import org.rust.lang.utils.evaluation.ConstExprEvaluator;
import org.rust.lang.core.types.LifetimeUtil;

public class TypeSubstitutingPsiRenderer extends RsPsiRenderer {

    private final Substitution subst;

    public TypeSubstitutingPsiRenderer(@NotNull PsiRenderingOptions options, @NotNull Substitution subst) {
        super(options);
        this.subst = subst;
    }

    @Override
    public void appendTypeReference(@NotNull StringBuilder sb, @NotNull RsTypeReference type) {
        Ty ty = org.rust.lang.core.types.ExtensionsUtil.getRawType(type);
        if (ty instanceof TyTypeParameter tyParam && subst.get(tyParam) != null) {
            sb.append(org.rust.lang.core.psi.ext.RsTypeReferenceUtil.substAndGetText(type, subst));
        } else {
            super.appendTypeReference(sb, type);
        }
    }

    @Override
    public void appendLifetime(@NotNull StringBuilder sb, @NotNull RsLifetime lifetime) {
        Region resolvedLifetime = org.rust.lang.core.types.infer.TyLowering.resolveLifetime(lifetime);
        Region substitutedLifetime = resolvedLifetime instanceof ReEarlyBound ? subst.get((ReEarlyBound) resolvedLifetime) : null;
        if (substitutedLifetime instanceof ReEarlyBound earlyBound) {
            sb.append(earlyBound.getParameter().getName());
        } else {
            sb.append(lifetime.getReferenceName());
        }
    }

    @Override
    public void appendConstExpr(@NotNull StringBuilder sb, @NotNull RsExpr expr, @NotNull Ty expectedTy) {
        var constVal = ConstExprEvaluator.evaluate(expr, expectedTy);
        if (constVal == null) { sb.append("{}"); return; }
        var substituted = org.rust.lang.core.types.infer.FoldUtil.substitute(constVal, subst);
        if (substituted instanceof CtValue ctValue) {
            sb.append(ctValue);
        } else if (substituted instanceof CtConstParameter ctParam) {
            boolean wrapParameterInBraces = org.rust.lang.core.psi.ext.PsiElementUtil.getStubParent(expr) instanceof RsTypeArgumentList;
            if (wrapParameterInBraces) sb.append("{ ");
            sb.append(ctParam);
            if (wrapParameterInBraces) sb.append(" }");
        } else {
            sb.append("{}");
        }
    }
}
