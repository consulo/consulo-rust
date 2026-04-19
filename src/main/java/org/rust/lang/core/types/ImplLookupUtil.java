/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.lang.core.types;

import org.jetbrains.annotations.NotNull;
import org.rust.lang.core.psi.RsTraitItem;
import org.rust.lang.core.resolve.SelectionCandidate;
import org.rust.lang.core.types.consts.CtConstParameter;
import org.rust.lang.core.types.infer.Obligation;
import org.rust.lang.core.types.infer.RsInferenceContext;
import org.rust.lang.core.types.ty.Ty;
import org.rust.lang.core.types.ty.TyTypeParameter;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Utility methods for ImplLookup operations.
 */
public final class ImplLookupUtil {
    private ImplLookupUtil() {
    }

    @NotNull
    public static SelectionCandidate.Triple<Substitution, TraitRef, List<Obligation>> prepareSubstAndTraitRefRaw(
        @NotNull RsInferenceContext ctx,
        @NotNull List<TyTypeParameter> generics,
        @NotNull List<CtConstParameter> constGenerics,
        @NotNull Ty formalSelfTy,
        @NotNull BoundElement<RsTraitItem> formalTrait,
        int recursionDepth
    ) {
        // Create fresh type variables for each generic parameter
        Map<TyTypeParameter, Ty> typeSubst = new HashMap<>();
        for (TyTypeParameter param : generics) {
            typeSubst.put(param, ctx.typeVarForParam(param));
        }
        Map<CtConstParameter, org.rust.lang.core.types.consts.Const> constSubst = new HashMap<>();
        for (CtConstParameter param : constGenerics) {
            constSubst.put(param, ctx.constVarForParam(param));
        }
        Substitution subst = new Substitution(typeSubst, Collections.emptyMap(), constSubst);

        // Apply substitution to formal self type and trait ref
        Ty selfTy = org.rust.lang.core.types.infer.FoldUtil.substitute(formalSelfTy, subst);
        BoundElement<RsTraitItem> trait = new BoundElement<>(
            formalTrait.getTypedElement(),
            formalTrait.getSubst().substituteInValues(subst),
            formalTrait.getAssoc()
        );
        TraitRef traitRef = new TraitRef(selfTy, trait);

        return new SelectionCandidate.Triple<>(subst, traitRef, Collections.emptyList());
    }
}
