/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.lang.core.types.infer;

import com.intellij.openapi.util.RecursionGuard;
import com.intellij.openapi.util.RecursionManager;
import com.intellij.psi.PsiElement;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.rust.lang.core.macros.MacroExpansion;
import org.rust.lang.core.psi.*;
import org.rust.lang.core.psi.ext.*;
import org.rust.lang.core.resolve.RsPathResolveResult;
import org.rust.lang.core.resolve.KnownItems;
import org.rust.lang.core.resolve.ref.RsPathReferenceImpl;
import org.rust.lang.core.types.*;
import org.rust.lang.core.types.consts.Const;
import org.rust.lang.core.types.consts.CtConstParameter;
import org.rust.lang.core.types.consts.CtUnknown;
import org.rust.lang.core.types.regions.ReEarlyBound;
import org.rust.lang.core.types.regions.ReStatic;
import org.rust.lang.core.types.regions.ReUnknown;
import org.rust.lang.core.types.regions.Region;
import org.rust.lang.core.types.ty.*;
import org.rust.lang.utils.evaluation.PathExprResolver;
import org.rust.lang.utils.evaluation.ConstExprUtil;

import java.util.*;

public class TyLowering {
    private static final RecursionGuard<PsiElement> GUARD =
        RecursionManager.createGuard("org.rust.lang.core.types.infer.TyLowering");

    @NotNull
    private final Map<RsPath, List<RsPathResolveResult<RsElement>>> myResolvedNestedPaths;
    @NotNull
    private final Map<RsTypeDeclarationElement, Ty> myDeclaredTypeCache = new HashMap<>();
    @NotNull
    private final Map<RsGenericDeclaration, List<RsGenericParameter>> myGenericParametersCache = new HashMap<>();

    private TyLowering(@NotNull Map<RsPath, List<RsPathResolveResult<RsElement>>> resolvedNestedPaths) {
        myResolvedNestedPaths = new HashMap<>(resolvedNestedPaths);
    }

    @NotNull
    private Ty lowerTy(@NotNull RsTypeReference type, @Nullable Region defaultTraitObjectRegion) {
        if (type instanceof RsParenType) {
            RsTypeReference inner = ((RsParenType) type).getTypeReference();
            return inner != null ? lowerTy(inner, defaultTraitObjectRegion) : TyUnknown.INSTANCE;
        }
        if (type instanceof RsTupleType) {
            List<RsTypeReference> refs = ((RsTupleType) type).getTypeReferenceList();
            List<Ty> types = new ArrayList<>(refs.size());
            for (RsTypeReference ref : refs) {
                types.add(lowerTy(ref, null));
            }
            return new TyTuple(types);
        }
        if (type instanceof RsUnitType) {
            return TyUnit.INSTANCE;
        }
        if (type instanceof RsNeverType) {
            return TyNever.INSTANCE;
        }
        if (type instanceof RsInferType) {
            return new TyPlaceholder((RsInferType) type);
        }
        if (type instanceof RsPathType) {
            return lowerPathType((RsPathType) type, defaultTraitObjectRegion);
        }
        if (type instanceof RsRefLikeType) {
            return lowerRefLikeType((RsRefLikeType) type, defaultTraitObjectRegion);
        }
        if (type instanceof RsArrayType) {
            return lowerArrayType((RsArrayType) type);
        }
        if (type instanceof RsFnPointerType) {
            return lowerFnPointerType((RsFnPointerType) type);
        }
        if (type instanceof RsTraitType) {
            return lowerTraitType((RsTraitType) type, defaultTraitObjectRegion);
        }
        if (type instanceof RsMacroType) {
            MacroExpansion expansion = RsPossibleMacroCallUtil.getExpansion(((RsMacroType) type).getMacroCall());
            if (expansion instanceof MacroExpansion.Type) {
                return lowerTy(((MacroExpansion.Type) expansion).getType(), null);
            }
            return TyUnknown.INSTANCE;
        }
        return TyUnknown.INSTANCE;
    }

    @NotNull
    private Ty lowerPathType(@NotNull RsPathType type, @Nullable Region defaultTraitObjectRegion) {
        RsPath path = type.getPath();
        List<RsPathResolveResult<RsElement>> rawResolveResult = rawMultiResolvePath(path);

        Ty primitiveType = TyPrimitive.fromPath(path, rawResolveResult);
        if (primitiveType != null) return primitiveType;

        if (rawResolveResult.size() != 1) return TyUnknown.INSTANCE;
        RsPathResolveResult<RsElement> singleResolveResult = rawResolveResult.get(0);
        RsElement target = singleResolveResult.element();
        BoundElement<?> boundElement = instantiatePathGenerics(
            path,
            singleResolveResult.element(),
            singleResolveResult.getResolvedSubst(),
            PathExprResolver.getDefault(),
            target instanceof RsTraitItem
        );

        if (target instanceof RsTraitOrImpl && RsPathUtil.getHasCself(path)) {
            if (target instanceof RsImplItem) {
                RsTypeReference typeReference = ((RsImplItem) target).getTypeReference();
                if (typeReference == null || containsElement(RsPathUtil.getContexts(path), typeReference)) {
                    return TyUnknown.INSTANCE;
                } else {
                    return myDeclaredTypeCache.computeIfAbsent((RsTypeDeclarationElement) target,
                        k -> lowerTy(typeReference, null));
                }
            } else {
                return myDeclaredTypeCache.computeIfAbsent((RsTypeDeclarationElement) target,
                    k -> TyTypeParameter.self((RsTraitOrImpl) target));
            }
        }
        if (target instanceof RsTraitItem) {
            @SuppressWarnings("unchecked")
            BoundElement<RsTraitItem> downcast = (BoundElement<RsTraitItem>) boundElement.downcast(RsTraitItem.class);
            List<BoundElement<RsTraitItem>> traits = downcast != null ? Collections.singletonList(downcast) : Collections.emptyList();
            Region region = defaultTraitObjectRegion != null ? defaultTraitObjectRegion : ReUnknown.INSTANCE;
            return new TyTraitObject(traits, region, false);
        }
        if (target instanceof RsTypeDeclarationElement) {
            Ty ty = myDeclaredTypeCache.computeIfAbsent((RsTypeDeclarationElement) target,
                k -> ((RsTypeDeclarationElement) target).getDeclaredType());
            ty = substituteWithTraitObjectRegion(ty, boundElement.getSubst(),
                defaultTraitObjectRegion != null ? defaultTraitObjectRegion : ReStatic.INSTANCE);
            if (target instanceof RsTypeAlias && !((RsTypeAlias) target).getOwner().isImplOrTrait()) {
                @SuppressWarnings("unchecked")
                BoundElement<RsTypeAlias> dc = (BoundElement<RsTypeAlias>) boundElement.downcast(RsTypeAlias.class);
                if (dc != null) return ty.withAlias(dc);
            }
            return ty;
        }
        return TyUnknown.INSTANCE;
    }

    @NotNull
    private Ty lowerRefLikeType(@NotNull RsRefLikeType type, @Nullable Region defaultTraitObjectRegion) {
        RsTypeReference base = type.getTypeReference();
        if (RsRefLikeTypeUtil.isRef(type)) {
            Region refRegion = resolveLifetime(type.getLifetime());
            Ty baseTy = base != null ? lowerTy(base, refRegion) : TyUnknown.INSTANCE;
            return new TyReference(baseTy, RsRefLikeTypeUtil.getMutability(type), refRegion);
        }
        if (RsRefLikeTypeUtil.isPointer(type)) {
            Ty baseTy = base != null ? lowerTy(base, ReStatic.INSTANCE) : TyUnknown.INSTANCE;
            return new TyPointer(baseTy, RsRefLikeTypeUtil.getMutability(type));
        }
        return TyUnknown.INSTANCE;
    }

    @NotNull
    private Ty lowerArrayType(@NotNull RsArrayType type) {
        RsTypeReference typeRef = type.getTypeReference();
        Ty componentType = typeRef != null ? lowerTy(typeRef, null) : TyUnknown.INSTANCE;
        if (RsArrayTypeUtil.isSlice(type)) {
            return new TySlice(componentType);
        } else {
            RsExpr expr = type.getExpr();
            Const c = expr != null ? ConstExprUtil.evaluate(expr, TyInteger.USize.INSTANCE) : CtUnknown.INSTANCE;
            return new TyArray(componentType, c);
        }
    }

    @NotNull
    private Ty lowerFnPointerType(@NotNull RsFnPointerType type) {
        RsValueParameterList paramList = type.getValueParameterList();
        List<RsValueParameter> params = paramList != null ? paramList.getValueParameterList() : Collections.emptyList();
        List<Ty> paramTypes = new ArrayList<>(params.size());
        for (RsValueParameter p : params) {
            RsTypeReference typeRef = p.getTypeReference();
            paramTypes.add(typeRef != null ? lowerTy(typeRef, null) : TyUnknown.INSTANCE);
        }
        RsRetType retType = type.getRetType();
        Ty retTy;
        if (retType != null) {
            RsTypeReference retTypeRef = retType.getTypeReference();
            retTy = retTypeRef != null ? lowerTy(retTypeRef, null) : TyUnknown.INSTANCE;
        } else {
            retTy = TyUnit.INSTANCE;
        }
        return new TyFunctionPointer(new FnSig(paramTypes, retTy, Unsafety.fromBoolean(RsFnPointerTypeUtil.isUnsafe(type))));
    }

    @NotNull
    private Ty lowerTraitType(@NotNull RsTraitType type, @Nullable Region defaultTraitObjectRegion) {
        boolean hasSizedUnbound = false;
        boolean hasUnresolvedBound = false;
        List<BoundElement<RsTraitItem>> traitBounds = new ArrayList<>();
        for (RsPolybound polybound : type.getPolyboundList()) {
            if (RsPolyboundUtil.getHasQ(polybound)) {
                hasSizedUnbound = true;
                continue;
            }
            RsBound bound = polybound.getBound();
            RsTraitRef traitRef = bound.getTraitRef();
            if (traitRef == null) continue;
            RsPath path = traitRef.getPath();
            if (path == null) continue;
            List<RsPathResolveResult<RsElement>> res = rawMultiResolvePath(path);
            if (res.size() != 1) {
                hasUnresolvedBound = true;
                continue;
            }
            RsPathResolveResult<RsElement> single = res.get(0);
            BoundElement<?> be = instantiatePathGenerics(path, single.element(), single.getResolvedSubst(),
                PathExprResolver.getDefault(), true);
            @SuppressWarnings("unchecked")
            BoundElement<RsTraitItem> downcast = (BoundElement<RsTraitItem>) be.downcast(RsTraitItem.class);
            if (downcast != null) {
                traitBounds.add(downcast);
            }
        }

        if (RsTraitTypeExtUtil.isImpl(type)) {
            RsTraitItem sized = KnownItems.getKnownItems(type).getSized();
            List<BoundElement<RsTraitItem>> boundsWithSized;
            if (!hasSizedUnbound && sized != null) {
                boundsWithSized = new ArrayList<>(traitBounds);
                boundsWithSized.add(new BoundElement<>(sized));
            } else {
                boundsWithSized = traitBounds;
            }
            return new TyAnon(type, boundsWithSized);
        }
        if (traitBounds.isEmpty()) return TyUnknown.INSTANCE;

        List<RsPolybound> polybounds = type.getPolyboundList();
        Region regionBound = null;
        for (RsPolybound pb : polybounds) {
            RsLifetime lt = pb.getBound().getLifetime();
            if (lt != null) {
                regionBound = resolveLifetime(lt);
                break;
            }
        }
        if (regionBound == null) {
            regionBound = defaultTraitObjectRegion != null ? defaultTraitObjectRegion : ReStatic.INSTANCE;
        }
        return new TyTraitObject(traitBounds, regionBound, hasUnresolvedBound);
    }

    @NotNull
    private List<RsPathResolveResult<RsElement>> rawMultiResolvePath(@NotNull RsPath path) {
        List<RsPathResolveResult<RsElement>> alreadyResolved = myResolvedNestedPaths.get(path);
        if (alreadyResolved == null) {
            myResolvedNestedPaths.putAll(RsPathReferenceImpl.resolveNeighborPaths(path));
        }
        List<RsPathResolveResult<RsElement>> result = myResolvedNestedPaths.get(path);
        return result != null ? result : Collections.emptyList();
    }

    @NotNull
    private <T extends RsElement> BoundElement<T> instantiatePathGenerics(
        @NotNull RsPath path,
        @NotNull T element,
        @NotNull Substitution subst,
        @NotNull PathExprResolver resolver,
        boolean withAssoc
    ) {
        if (!(element instanceof RsGenericDeclaration)) return new BoundElement<>(element, subst);

        RsGenericDeclaration genericDecl = (RsGenericDeclaration) element;
        List<RsGenericParameter> genericParameters = myGenericParametersCache.computeIfAbsent(genericDecl,
            k -> RsGenericDeclarationUtil.getGenericParameters(k));
        RsPsiSubstitution psiSubstitution = org.rust.lang.core.resolve.ref.PathPsiSubstUtil.pathPsiSubst(path, genericDecl, genericParameters);

        Map<TyTypeParameter, Ty> typeSubst = new HashMap<>();
        for (Map.Entry<RsTypeParameter, RsPsiSubstitution.Value<RsPsiSubstitution.TypeValue, RsPsiSubstitution.TypeDefault>> entry : psiSubstitution.getTypeSubst().entrySet()) {
            TyTypeParameter paramTy = TyTypeParameter.named(entry.getKey());
            RsPsiSubstitution.Value<RsPsiSubstitution.TypeValue, RsPsiSubstitution.TypeDefault> value = entry.getValue();
            Ty valueTy;
            if (value instanceof RsPsiSubstitution.Value.DefaultValue) {
                RsPsiSubstitution.TypeDefault defaultVal = ((RsPsiSubstitution.Value.DefaultValue<RsPsiSubstitution.TypeValue, RsPsiSubstitution.TypeDefault>) value).getValue();
                RsTypeReference defaultValue = defaultVal.getValue();
                Ty defaultValueTy = GUARD.doPreventingRecursion(defaultValue, true, () -> ExtensionsUtil.getRawType(defaultValue));
                if (defaultValueTy == null) defaultValueTy = TyUnknown.INSTANCE;
                if (defaultVal.getSelfTy() != null) {
                    Map<TyTypeParameter, Ty> selfMap = new HashMap<>();
                    selfMap.put(TyTypeParameter.self(), defaultVal.getSelfTy());
                    defaultValueTy = FoldUtil.substitute(defaultValueTy, SubstitutionUtil.toTypeSubst(selfMap));
                }
                valueTy = FoldUtil.substitute(defaultValueTy, SubstitutionUtil.toTypeSubst(typeSubst));
            } else if (value instanceof RsPsiSubstitution.Value.OptionalAbsent) {
                valueTy = paramTy;
            } else if (value instanceof RsPsiSubstitution.Value.Present) {
                RsPsiSubstitution.TypeValue tv = ((RsPsiSubstitution.Value.Present<RsPsiSubstitution.TypeValue, RsPsiSubstitution.TypeDefault>) value).getValue();
                if (tv instanceof RsPsiSubstitution.TypeValue.InAngles) {
                    valueTy = lowerTy(((RsPsiSubstitution.TypeValue.InAngles) tv).getValue(), null);
                } else if (tv instanceof RsPsiSubstitution.TypeValue.FnSugar) {
                    List<RsTypeReference> inputArgs = ((RsPsiSubstitution.TypeValue.FnSugar) tv).getInputArgs();
                    if (!inputArgs.isEmpty()) {
                        List<Ty> argTys = new ArrayList<>(inputArgs.size());
                        for (RsTypeReference arg : inputArgs) {
                            argTys.add(arg != null ? lowerTy(arg, null) : TyUnknown.INSTANCE);
                        }
                        valueTy = new TyTuple(argTys);
                    } else {
                        valueTy = TyUnit.INSTANCE;
                    }
                } else {
                    valueTy = TyUnknown.INSTANCE;
                }
            } else {
                // RequiredAbsent
                valueTy = TyUnknown.INSTANCE;
            }
            typeSubst.put(paramTy, valueTy);
        }

        Map<ReEarlyBound, Region> regionSubst = new HashMap<>();
        for (Map.Entry<RsLifetimeParameter, RsPsiSubstitution.Value<RsLifetime, ?>> entry : psiSubstitution.getRegionSubst().entrySet()) {
            ReEarlyBound param = new ReEarlyBound(entry.getKey());
            RsPsiSubstitution.Value<RsLifetime, ?> psiValue = entry.getValue();
            if (psiValue instanceof RsPsiSubstitution.Value.Present) {
                Region region = resolveLifetime(((RsPsiSubstitution.Value.Present<RsLifetime, Object>) psiValue).getValue());
                regionSubst.put(param, region);
            }
        }

        Map<CtConstParameter, Const> constSubst = new HashMap<>();
        for (Map.Entry<RsConstParameter, RsPsiSubstitution.Value<RsElement, RsExpr>> entry : psiSubstitution.getConstSubst().entrySet()) {
            RsConstParameter psiParam = entry.getKey();
            CtConstParameter param = new CtConstParameter(psiParam);
            RsPsiSubstitution.Value<RsElement, RsExpr> psiValue = entry.getValue();
            Const constVal;
            if (psiValue instanceof RsPsiSubstitution.Value.OptionalAbsent) {
                constVal = param;
            } else if (psiValue instanceof RsPsiSubstitution.Value.Present) {
                RsTypeReference paramTypeRef = psiParam.getTypeReference();
                Ty expectedTy = paramTypeRef != null ? ExtensionsUtil.getNormType(paramTypeRef) : TyUnknown.INSTANCE;
                constVal = ConstExprUtil.toConst(((RsPsiSubstitution.Value.Present<RsElement, RsExpr>) psiValue).getValue(), expectedTy, resolver);
            } else if (psiValue instanceof RsPsiSubstitution.Value.DefaultValue) {
                RsTypeReference paramTypeRef = psiParam.getTypeReference();
                Ty expectedTy = paramTypeRef != null ? ExtensionsUtil.getNormType(paramTypeRef) : TyUnknown.INSTANCE;
                Const c = ConstExprUtil.toConst(((RsPsiSubstitution.Value.DefaultValue<RsElement, RsExpr>) psiValue).getValue(), expectedTy, resolver);
                constVal = FoldUtil.substitute(c, SubstitutionUtil.toConstSubst(constSubst));
            } else {
                // RequiredAbsent
                constVal = CtUnknown.INSTANCE;
            }
            constSubst.put(param, constVal);
        }

        Substitution newSubst = new Substitution(typeSubst, regionSubst, constSubst);

        Map<RsTypeAlias, Ty> assoc;
        if (withAssoc) {
            assoc = new HashMap<>();
            for (Map.Entry<RsTypeAlias, RsPsiSubstitution.AssocValue> entry : psiSubstitution.getAssoc().entrySet()) {
                RsPsiSubstitution.AssocValue val = entry.getValue();
                if (val instanceof RsPsiSubstitution.AssocValue.Present) {
                    assoc.put(entry.getKey(), lowerTy(((RsPsiSubstitution.AssocValue.Present) val).getValue(), null));
                } else if (val == RsPsiSubstitution.AssocValue.FnSugarImplicitRet.INSTANCE) {
                    assoc.put(entry.getKey(), TyUnit.INSTANCE);
                }
            }
        } else {
            assoc = Collections.emptyMap();
        }

        return new BoundElement<>(element, subst.plus(newSubst), assoc);
    }

    // --- Companion / static methods ---

    @NotNull
    public static Ty lowerTypeReference(@NotNull RsTypeReference type) {
        Ty ty = new TyLowering(Collections.emptyMap()).lowerTy(type, null);
        return FoldUtil.foldTyPlaceholderWithTyInfer(ty);
    }

    @NotNull
    public static <T extends RsElement> BoundElement<T> lowerPathGenerics(
        @NotNull RsPath path,
        @NotNull T element,
        @NotNull Substitution subst,
        @NotNull PathExprResolver resolver,
        @NotNull Map<RsPath, List<RsPathResolveResult<RsElement>>> resolvedNestedPaths
    ) {
        BoundElement<T> be = new TyLowering(resolvedNestedPaths)
            .instantiatePathGenerics(path, element, subst, resolver, true);
        return FoldUtil.foldTyPlaceholderWithTyInfer(be);
    }

    // --- Extension functions converted to static ---

    @NotNull
    public static Region resolveLifetime(@Nullable RsLifetime lifetime) {
        if (lifetime == null) return ReUnknown.INSTANCE;
        if ("'static".equals(lifetime.getReferenceName())) return ReStatic.INSTANCE;
        Object resolved = lifetime.getReference().resolve();
        if (resolved instanceof RsLifetimeParameter) return new ReEarlyBound((RsLifetimeParameter) resolved);
        return ReUnknown.INSTANCE;
    }

    @NotNull
    @SuppressWarnings("unchecked")
    private static <T extends TypeFoldable<T>> T substituteWithTraitObjectRegion(
        @NotNull Object ty,
        @NotNull Substitution subst,
        @NotNull Region defaultTraitObjectRegion
    ) {
        TypeFoldable<?> foldable = (TypeFoldable<?>) ty;
        Object result = foldable.foldWith(new TypeFolder() {
            @NotNull
            @Override
            public Ty foldTy(@NotNull Ty t) {
                if (t instanceof TyTypeParameter) {
                    Ty handled = handleTraitObject((TyTypeParameter) t);
                    return handled != null ? handled : t;
                }
                if (FoldUtil.needsSubst(t)) return t.superFoldWith(this);
                return t;
            }

            @NotNull
            @Override
            public Region foldRegion(@NotNull Region region) {
                if (region instanceof ReEarlyBound) {
                    Region substituted = subst.get((ReEarlyBound) region);
                    return substituted != null ? substituted : region;
                }
                return region;
            }

            @NotNull
            @Override
            public Const foldConst(@NotNull Const c) {
                if (c instanceof CtConstParameter) {
                    Const substituted = subst.get((CtConstParameter) c);
                    return substituted != null ? substituted : c;
                }
                if (FoldUtil.hasCtConstParameters(c)) return c.superFoldWith(this);
                return c;
            }

            @Nullable
            private Ty handleTraitObject(@NotNull TyTypeParameter paramTy) {
                Ty t = subst.get(paramTy);
                if (!(t instanceof TyTraitObject) || !(((TyTraitObject) t).getRegion() instanceof ReUnknown)) return t;
                TyTraitObject traitObj = (TyTraitObject) t;
                List<Region> bounds = paramTy.getRegionBounds();
                Region region;
                if (bounds.size() == 0) {
                    region = defaultTraitObjectRegion;
                } else if (bounds.size() == 1) {
                    region = FoldUtil.substitute(bounds.get(0), subst);
                } else {
                    region = ReUnknown.INSTANCE;
                }
                return new TyTraitObject(traitObj.getTraits(), region, traitObj.getHasUnresolvedBound());
            }
        });
        return (T) result;
    }

    private static boolean containsElement(@NotNull Iterable<PsiElement> iterable, @NotNull PsiElement element) {
        for (PsiElement e : iterable) {
            if (e == element) return true;
        }
        return false;
    }
}
