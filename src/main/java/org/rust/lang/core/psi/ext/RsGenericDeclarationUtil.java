/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.lang.core.psi.ext;

import com.intellij.psi.util.CachedValueProvider;
import com.intellij.psi.util.CachedValuesManager;
import org.jetbrains.annotations.NotNull;
import org.rust.lang.core.psi.*;
import org.rust.lang.core.resolve.KnownItems;
import org.rust.lang.core.types.BoundElement;
import org.rust.lang.core.types.ExtensionsUtil;
import org.rust.lang.core.types.Substitution;
import org.rust.lang.core.types.TraitRef;
import org.rust.lang.core.types.consts.Const;
import org.rust.lang.core.types.consts.CtConstParameter;
import org.rust.lang.core.types.infer.BoundConstness;
import org.rust.lang.core.types.infer.Predicate;
import org.rust.lang.core.types.regions.ReEarlyBound;
import org.rust.lang.core.types.regions.Region;
import org.rust.lang.core.types.ty.Ty;
import org.rust.lang.core.types.ty.TyProjection;
import org.rust.lang.core.types.ty.TyTypeParameter;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public final class RsGenericDeclarationUtil {

    private RsGenericDeclarationUtil() {
    }

    @NotNull
    public static List<RsTypeParameter> getTypeParameters(@NotNull RsGenericDeclaration decl) {
        RsTypeParameterList list = decl.getTypeParameterList();
        return list != null ? list.getTypeParameterList() : Collections.emptyList();
    }

    @NotNull
    public static List<RsLifetimeParameter> getLifetimeParameters(@NotNull RsGenericDeclaration decl) {
        RsTypeParameterList list = decl.getTypeParameterList();
        return list != null ? list.getLifetimeParameterList() : Collections.emptyList();
    }

    @NotNull
    public static List<RsConstParameter> getConstParameters(@NotNull RsGenericDeclaration decl) {
        RsTypeParameterList list = decl.getTypeParameterList();
        return list != null ? list.getConstParameterList() : Collections.emptyList();
    }

    @NotNull
    public static List<RsGenericParameter> getRequiredGenericParameters(@NotNull RsGenericDeclaration decl) {
        List<RsGenericParameter> result = new ArrayList<>();
        for (RsGenericParameter param : getGenericParameters(decl)) {
            if (param instanceof RsTypeParameter) {
                if (((RsTypeParameter) param).getTypeReference() == null) result.add(param);
            } else if (param instanceof RsConstParameter) {
                if (((RsConstParameter) param).getExpr() == null) result.add(param);
            }
        }
        return result;
    }

    @NotNull
    public static List<RsGenericParameter> getGenericParameters(@NotNull RsGenericDeclaration decl) {
        RsTypeParameterList list = decl.getTypeParameterList();
        return list != null ? RsTypeParameterListUtil.getGenericParameters(list) : Collections.emptyList();
    }

    @NotNull
    public static List<RsWherePred> getWherePreds(@NotNull RsGenericDeclaration decl) {
        if (decl instanceof RsTypeAlias
            && RsAbstractableImplUtil.getOwner((RsTypeAlias) decl) instanceof RsAbstractableOwner.Impl) {
            List<RsWherePred> result = new ArrayList<>();
            for (RsWhereClause wc : ((RsTypeAlias) decl).getWhereClauseList()) {
                result.addAll(wc.getWherePredList());
            }
            return result;
        }
        RsWhereClause wc = decl.getWhereClause();
        return wc != null ? wc.getWherePredList() : Collections.emptyList();
    }

    /**
     * Returns the trait bounds of this declaration, derived from its predicates.
     * {@code predicates.mapNotNull { (it as? Predicate.Trait)?.trait }}.
     */
    @NotNull
    public static List<TraitRef> getBounds(@NotNull RsGenericDeclaration decl) {
        List<TraitRef> result = new ArrayList<>();
        for (Predicate p : getPredicates(decl)) {
            if (p instanceof Predicate.Trait) {
                result.add(((Predicate.Trait) p).getTrait());
            }
        }
        return result;
    }

    @NotNull
    public static List<Predicate> getPredicates(@NotNull RsGenericDeclaration decl) {
        return CachedValuesManager.getCachedValue(decl, () -> CachedValueProvider.Result.create(
            doGetPredicates(decl),
            RsPsiManagerUtil.getRustStructureOrAnyPsiModificationTracker(decl)
        ));
    }

    @NotNull
    private static List<Predicate> doGetPredicates(@NotNull RsGenericDeclaration decl) {
        // where-clause bounds: for each where predicate, the self type is its type-reference's raw type.
        List<PsiPredicate> whereBounds = new ArrayList<>();
        for (RsWherePred pred : getWherePreds(decl)) {
            RsTypeReference typeRef = pred.getTypeReference();
            if (typeRef == null) continue;
            Ty selfTy = ExtensionsUtil.getRawType(typeRef);
            RsTypeParamBounds bounds = pred.getTypeParamBounds();
            toPredicates(bounds != null ? bounds.getPolyboundList() : Collections.emptyList(), selfTy, whereBounds);
        }

        // Inline type-parameter bounds: `T: Trait` declared in the generics list.
        List<PsiPredicate> typeParamBounds = new ArrayList<>();
        for (RsTypeParameter tp : getTypeParameters(decl)) {
            Ty selfTy = TyTypeParameter.named(tp);
            RsTypeParamBounds bounds = tp.getTypeParamBounds();
            toPredicates(bounds != null ? bounds.getPolyboundList() : Collections.emptyList(), selfTy, typeParamBounds);
        }

        // Associated type bounds on trait items: `trait T { type A: Debug; }` implies `Self::A: Debug`.
        List<TyProjection> assocTypes;
        List<PsiPredicate> assocTypeBounds = new ArrayList<>();
        if (decl instanceof RsTraitItem) {
            assocTypes = new ArrayList<>();
            RsTraitItem traitItem = (RsTraitItem) decl;
            for (RsAbstractable m : RsTraitOrImplUtil.getExpandedMembers(traitItem)) {
                if (!(m instanceof RsTypeAlias)) continue;
                RsTypeAlias alias = (RsTypeAlias) m;
                assocTypes.add(TyProjection.valueOf(withDefaultSubst(alias)));
            }
            for (TyProjection proj : assocTypes) {
                RsTypeAlias alias = proj.getTarget().getTypedElement();
                RsTypeParamBounds bounds = alias.getTypeParamBounds();
                toPredicates(bounds != null ? bounds.getPolyboundList() : Collections.emptyList(), proj, assocTypeBounds);
            }
        } else {
            assocTypes = Collections.emptyList();
        }

        List<PsiPredicate> explicitPredicates = new ArrayList<>();
        explicitPredicates.addAll(typeParamBounds);
        explicitPredicates.addAll(whereBounds);
        explicitPredicates.addAll(assocTypeBounds);

        RsTraitItem sized = KnownItems.getKnownItems(decl).getSized();
        List<Predicate> implicitPredicates = new ArrayList<>();
        if (sized != null) {
            // Collect types that are explicitly marked `?Sized` (Unbound) or `T: Sized` (Bound) so we
            // don't re-add an implicit `Sized` bound for them.
            Set<Ty> sizedBounds = new HashSet<>();
            for (PsiPredicate pp : explicitPredicates) {
                if (pp instanceof PsiPredicate.Unbound) {
                    sizedBounds.add(((PsiPredicate.Unbound) pp).selfTy);
                } else if (pp instanceof PsiPredicate.Bound) {
                    Predicate inner = ((PsiPredicate.Bound) pp).predicate;
                    if (inner instanceof Predicate.Trait) {
                        TraitRef tr = ((Predicate.Trait) inner).getTrait();
                        if (tr.getTrait().getTypedElement() == sized) {
                            sizedBounds.add(tr.getSelfTy());
                        }
                    }
                }
            }

            // Implicit `Sized` bound for every type parameter and every associated type that isn't opted out.
            BoundElement<RsTraitItem> sizedBE = withSubst(sized);
            for (RsTypeParameter tp : getTypeParameters(decl)) {
                Ty selfTy = TyTypeParameter.named(tp);
                if (sizedBounds.contains(selfTy)) continue;
                implicitPredicates.add(new Predicate.Trait(new TraitRef(selfTy, sizedBE)));
            }
            for (TyProjection proj : assocTypes) {
                if (sizedBounds.contains(proj)) continue;
                implicitPredicates.add(new Predicate.Trait(new TraitRef(proj, sizedBE)));
            }
        }

        List<Predicate> result = new ArrayList<>(explicitPredicates.size() + implicitPredicates.size());
        for (PsiPredicate pp : explicitPredicates) {
            if (pp instanceof PsiPredicate.Bound) result.add(((PsiPredicate.Bound) pp).predicate);
        }
        result.addAll(implicitPredicates);
        return result;
    }

    /**
     * Translate a list of {@link RsPolybound} into {@link PsiPredicate}s with the given self type.
     */
    private static void toPredicates(@NotNull List<RsPolybound> polybounds,
                                     @NotNull Ty selfTy,
                                     @NotNull List<PsiPredicate> out) {
        for (RsPolybound bound : polybounds) {
            if (RsPolyboundUtil.getHasQ(bound)) { // ?Sized
                out.add(new PsiPredicate.Unbound(selfTy));
                continue;
            }
            RsTraitRef traitRef = bound.getBound().getTraitRef();
            if (traitRef == null) continue;
            BoundElement<RsTraitItem> boundTrait = RsTraitRefUtil.resolveToBoundTrait(traitRef);
            if (boundTrait == null) continue;

            BoundConstness constness = RsPolyboundUtil.getHasConst(bound)
                ? BoundConstness.ConstIfConst
                : BoundConstness.NotConst;
            out.add(new PsiPredicate.Bound(new Predicate.Trait(new TraitRef(selfTy, boundTrait), constness)));

            // Associated-type bindings inside the traitRef path: `Iterator<Item = Foo>` / `Iterator<Item: Debug>`.
            for (RsAssocTypeBinding binding : RsMethodOrPathUtil.getAssocTypeBindings(traitRef.getPath())) {
                BoundElement<RsTypeAlias> assoc = RsAssocTypeBindingUtil.resolveToBoundAssocType(binding);
                if (assoc == null) continue;
                TyProjection projectionTy = TyProjection.valueOf(selfTy, assoc);
                RsTypeReference typeRef = binding.getTypeReference();
                if (typeRef != null) {
                    // T: Iterator<Item = Foo>  =>  `T::Item == Foo`
                    out.add(new PsiPredicate.Bound(
                        new Predicate.Equate(projectionTy, ExtensionsUtil.getRawType(typeRef))
                    ));
                } else {
                    // T: Iterator<Item: Debug>  =>  `T::Item: Debug`
                    toPredicates(binding.getPolyboundList(), projectionTy, out);
                }
            }
        }
    }

    private abstract static class PsiPredicate {
        private PsiPredicate() {
        }

        static final class Bound extends PsiPredicate {
            @NotNull final Predicate predicate;

            Bound(@NotNull Predicate predicate) {
                this.predicate = predicate;
            }
        }

        static final class Unbound extends PsiPredicate {
            @NotNull final Ty selfTy;

            Unbound(@NotNull Ty selfTy) {
                this.selfTy = selfTy;
            }
        }
    }

    @NotNull
    public static <T extends RsGenericDeclaration> BoundElement<T> withSubst(@NotNull T decl, @NotNull Ty... subst) {
        return new BoundElement<>(decl).withSubst(subst);
    }

    @NotNull
    public static <T extends RsGenericDeclaration> BoundElement<T> withDefaultSubst(@NotNull T decl) {
        Map<TyTypeParameter, Ty> typeSubst = new HashMap<>();
        for (RsTypeParameter p : getTypeParameters(decl)) {
            TyTypeParameter param = TyTypeParameter.named(p);
            typeSubst.put(param, param);
        }
        Map<ReEarlyBound, Region> regionSubst = new HashMap<>();
        for (RsLifetimeParameter p : getLifetimeParameters(decl)) {
            ReEarlyBound region = new ReEarlyBound(p);
            regionSubst.put(region, region);
        }
        Map<CtConstParameter, Const> constSubst = new HashMap<>();
        for (RsConstParameter p : getConstParameters(decl)) {
            CtConstParameter c = new CtConstParameter(p);
            constSubst.put(c, c);
        }
        return new BoundElement<>(decl, new Substitution(typeSubst, regionSubst, constSubst));
    }
}
