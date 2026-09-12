/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.lang.core.resolve.ref;

import consulo.language.psi.PsiElement;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import org.rust.lang.core.psi.*;
import org.rust.lang.core.psi.ext.PsiElementUtil;
import org.rust.lang.core.psi.ext.RsElement;
import org.rust.lang.core.psi.ext.RsGenericDeclaration;
import org.rust.lang.core.psi.ext.RsGenericDeclarationUtil;
import org.rust.lang.core.psi.ext.RsPathUtil;
import org.rust.lang.core.psi.ext.RsTraitItemUtil;
import org.rust.lang.core.resolve.KnownItems;
import org.rust.lang.core.types.ExtensionsUtil;
import org.rust.lang.core.types.RsPsiSubstitution;
import org.rust.lang.core.types.RsPsiSubstitution.AssocValue;
import org.rust.lang.core.types.RsPsiSubstitution.TypeDefault;
import org.rust.lang.core.types.RsPsiSubstitution.TypeValue;
import org.rust.lang.core.types.RsPsiSubstitution.Value;
import org.rust.lang.core.types.ty.Ty;
import org.rust.lang.core.types.ty.TyUnknown;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

/**
 * Maps the generic parameters of a resolved declaration onto the generic arguments written in a path,
 * at the PSI level (see {@link RsPsiSubstitution}).
 */
public final class PathPsiSubstUtil {
    private PathPsiSubstUtil() {
    }

    @Nonnull
    public static RsPsiSubstitution pathPsiSubst(@Nonnull RsPath path, @Nonnull RsGenericDeclaration declaration) {
        return pathPsiSubst(path, declaration, null);
    }

    @Nonnull
    public static RsPsiSubstitution pathPsiSubst(
        @Nonnull RsPath path,
        @Nonnull RsGenericDeclaration declaration,
        @Nullable List<?> givenGenericParameters
    ) {
        if (RsPathUtil.getHasCself(path)) {
            return new RsPsiSubstitution();
        }

        PathParameters args = pathTypeParameters(path);

        List<RsLifetimeParameter> lifetimeParameters = new ArrayList<>();
        List<RsElement> typeOrConstParameters = new ArrayList<>();
        List<?> generics = givenGenericParameters != null
            ? givenGenericParameters
            : RsGenericDeclarationUtil.getGenericParameters(declaration);
        for (Object generic : generics) {
            if (generic instanceof RsLifetimeParameter) {
                lifetimeParameters.add((RsLifetimeParameter) generic);
            }
            else if (generic instanceof RsElement) {
                typeOrConstParameters.add((RsElement) generic);
            }
        }

        PsiElement parent = path.getParent();

        // Generic arguments are optional in expression context, e.g.
        // `let a = Foo::<u8>::bar::<u16>();` can be written as `let a = Foo::bar();`
        // if it is possible to infer `u8` and `u16` during type inference
        boolean areOptionalArgs = parent instanceof RsExpr
            || parent instanceof RsPath && parent.getParent() instanceof RsExpr;

        List<RsLifetime> lifetimeArgs = args instanceof PathParameters.InAngles
            ? ((PathParameters.InAngles) args).lifetimeArgs
            : null;
        Map<RsLifetimeParameter, Value<RsLifetime, Void>> rawRegionSubst =
            associateSubst(lifetimeParameters, lifetimeArgs, areOptionalArgs, p -> null);
        @SuppressWarnings({"unchecked", "rawtypes"})
        Map<RsLifetimeParameter, Value<RsLifetime, ?>> regionSubst = (Map) rawRegionSubst;

        List<Object> typeOrConstArguments = null;
        if (args instanceof PathParameters.InAngles) {
            typeOrConstArguments = new ArrayList<>();
            for (RsElement arg : ((PathParameters.InAngles) args).typeOrConstArgs) {
                if (arg instanceof RsTypeReference) {
                    typeOrConstArguments.add(new TypeValue.InAngles((RsTypeReference) arg));
                }
                else {
                    typeOrConstArguments.add(arg);
                }
            }
        }
        else if (args instanceof PathParameters.FnSugar) {
            typeOrConstArguments =
                Collections.singletonList(new TypeValue.FnSugar(((PathParameters.FnSugar) args).inputArgs));
        }

        Map<RsElement, Value<Object, Object>> typeOrConstSubst = associateSubst(
            typeOrConstParameters,
            typeOrConstArguments,
            areOptionalArgs,
            param -> defaultValueOf(param, parent)
        );

        Map<RsTypeParameter, Value<TypeValue, TypeDefault>> typeSubst = new LinkedHashMap<>();
        Map<RsConstParameter, Value<RsElement, RsExpr>> constSubst = new LinkedHashMap<>();
        for (Map.Entry<RsElement, Value<Object, Object>> entry : typeOrConstSubst.entrySet()) {
            RsElement param = entry.getKey();
            Value<Object, Object> value = entry.getValue();
            Object present = value instanceof Value.Present ? ((Value.Present<Object, Object>) value).getValue() : null;
            Object defaultValue =
                value instanceof Value.DefaultValue ? ((Value.DefaultValue<Object, Object>) value).getValue() : null;
            boolean isAbsent = value instanceof Value.RequiredAbsent || value instanceof Value.OptionalAbsent;

            if (param instanceof RsTypeParameter) {
                if (present instanceof TypeValue || defaultValue instanceof TypeDefault || isAbsent) {
                    @SuppressWarnings({"unchecked", "rawtypes"})
                    Value<TypeValue, TypeDefault> typed = (Value) value;
                    typeSubst.put((RsTypeParameter) param, typed);
                }
            }
            else if (param instanceof RsConstParameter) {
                if (present instanceof RsExpr || defaultValue instanceof RsExpr || isAbsent) {
                    @SuppressWarnings({"unchecked", "rawtypes"})
                    Value<RsElement, RsExpr> typed = (Value) value;
                    constSubst.put((RsConstParameter) param, typed);
                }
                else if (present instanceof TypeValue.InAngles
                    && ((TypeValue.InAngles) present).getValue() instanceof RsPathType) {
                    // `Foo<BAR>` parses `BAR` as a type, but the parameter it fills is a const parameter
                    constSubst.put((RsConstParameter) param, new Value.Present<>(((TypeValue.InAngles) present).getValue()));
                }
            }
        }

        Map<RsTypeAlias, AssocValue> assocTypes = new LinkedHashMap<>();
        if (declaration instanceof RsTraitItem) {
            RsTraitItem trait = (RsTraitItem) declaration;
            if (args instanceof PathParameters.InAngles) {
                // `Iterator<Item=T>`
                for (RsAssocTypeBinding binding : ((PathParameters.InAngles) args).assoc) {
                    // We can't just use `binding.reference.resolve()` here because resolving of an assoc type
                    // depends on the parent path resolve, so we would come back here and enter infinite recursion
                    RsTypeAlias assoc = RsPathReferenceImpl.resolveAssocTypeBinding(trait, binding);
                    if (assoc == null) continue;
                    RsTypeReference typeReference = binding.getTypeReference();
                    if (typeReference != null) {
                        assocTypes.put(assoc, new AssocValue.Present(typeReference));
                    }
                }
            }
            else if (args instanceof PathParameters.FnSugar) {
                // `Fn() -> T`
                RsTraitItem fnOnce = KnownItems.getKnownItems(path).getFnOnce();
                RsTypeAlias outputParam = fnOnce != null ? RsTraitItemUtil.findAssociatedType(fnOnce, "Output") : null;
                if (outputParam != null) {
                    RsTypeReference outputArg = ((PathParameters.FnSugar) args).outputArg;
                    assocTypes.put(outputParam, outputArg != null
                        ? new AssocValue.Present(outputArg)
                        : AssocValue.FnSugarImplicitRet.INSTANCE);
                }
            }
        }

        return new RsPsiSubstitution(typeSubst, regionSubst, constSubst, assocTypes);
    }

    /** The default value declared for a type or const parameter, or {@code null} if it has none. */
    @Nullable
    private static Object defaultValueOf(@Nonnull RsElement param, @Nullable PsiElement pathParent) {
        if (param instanceof RsTypeParameter) {
            RsTypeReference defaultTy = ((RsTypeParameter) param).getTypeReference();
            if (defaultTy == null) return null;
            return new TypeDefault(defaultTy, selfTyForDefault(pathParent));
        }
        if (param instanceof RsConstParameter) {
            return ((RsConstParameter) param).getExpr();
        }
        return null;
    }

    /**
     * A default type argument of a trait used as a bound may refer to {@code Self}, which then denotes the
     * bounded type, e.g. {@code T: PartialEq} desugars to {@code T: PartialEq<T>}.
     */
    @Nullable
    private static Ty selfTyForDefault(@Nullable PsiElement pathParent) {
        if (!(pathParent instanceof RsTraitRef)) return null;
        PsiElement bound = pathParent.getParent();
        if (!(bound instanceof RsBound)) return null;
        PsiElement polybound = bound.getParent();
        if (!(polybound instanceof RsPolybound)) return null;
        PsiElement bounds = polybound.getParent();
        if (!(bounds instanceof RsTypeParamBounds)) return null;
        PsiElement owner = bounds.getParent();
        if (owner instanceof RsWherePred) {
            RsTypeReference typeReference = ((RsWherePred) owner).getTypeReference();
            return typeReference != null ? ExtensionsUtil.getRawType(typeReference) : TyUnknown.INSTANCE;
        }
        if (owner instanceof RsTypeParameter) {
            return ((RsTypeParameter) owner).getDeclaredType();
        }
        return null;
    }

    /**
     * Pairs each parameter with the argument at the same position. A parameter with no argument gets its
     * declared default, or is reported absent — optionally so when arguments may be inferred instead.
     */
    @Nonnull
    private static <Param, P, D> Map<Param, Value<P, D>> associateSubst(
        @Nonnull List<Param> parameters,
        @Nullable List<? extends P> arguments,
        boolean areOptionalArgs,
        @Nonnull Function<Param, D> defaultOf
    ) {
        Map<Param, Value<P, D>> result = new LinkedHashMap<>();
        for (int i = 0; i < parameters.size(); i++) {
            Param param = parameters.get(i);
            Value<P, D> value;
            if (areOptionalArgs && arguments == null) {
                value = Value.OptionalAbsent.instance();
            }
            else if (arguments != null && i < arguments.size()) {
                value = new Value.Present<>(arguments.get(i));
            }
            else {
                D defaultValue = defaultOf.apply(param);
                value = defaultValue != null
                    ? new Value.DefaultValue<>(defaultValue)
                    : Value.RequiredAbsent.instance();
            }
            result.put(param, value);
        }
        return result;
    }

    /** The generic arguments written in a path, either {@code Foo<..>} or the {@code Fn(..) -> ..} sugar. */
    private abstract static class PathParameters {
        private PathParameters() {
        }

        /** {@code Foo<'a, Bar, Baz, 2+2, Item=i32>} */
        static final class InAngles extends PathParameters {
            final List<RsLifetime> lifetimeArgs;
            /** {@link RsTypeReference} or {@link RsExpr} */
            final List<RsElement> typeOrConstArgs;
            final List<RsAssocTypeBinding> assoc;

            InAngles(
                @Nonnull List<RsLifetime> lifetimeArgs,
                @Nonnull List<RsElement> typeOrConstArgs,
                @Nonnull List<RsAssocTypeBinding> assoc
            ) {
                this.lifetimeArgs = lifetimeArgs;
                this.typeOrConstArgs = typeOrConstArgs;
                this.assoc = assoc;
            }
        }

        /** {@code Fn(i32, i32) -> i32} */
        static final class FnSugar extends PathParameters {
            final List<RsTypeReference> inputArgs;
            @Nullable
            final RsTypeReference outputArg;

            FnSugar(@Nonnull List<RsTypeReference> inputArgs, @Nullable RsTypeReference outputArg) {
                this.inputArgs = inputArgs;
                this.outputArg = outputArg;
            }
        }
    }

    @Nullable
    private static PathParameters pathTypeParameters(@Nonnull RsPath path) {
        RsTypeArgumentList inAngles = path.getTypeArgumentList();
        if (inAngles != null) {
            List<RsElement> typeOrConstArgs = new ArrayList<>();
            List<RsLifetime> lifetimeArgs = new ArrayList<>();
            List<RsAssocTypeBinding> assoc = new ArrayList<>();
            for (RsElement child : PsiElementUtil.stubChildrenOfType(inAngles, RsElement.class)) {
                if (child instanceof RsTypeReference || child instanceof RsExpr) {
                    typeOrConstArgs.add(child);
                }
                else if (child instanceof RsLifetime) {
                    lifetimeArgs.add((RsLifetime) child);
                }
                else if (child instanceof RsAssocTypeBinding) {
                    assoc.add((RsAssocTypeBinding) child);
                }
            }
            return new PathParameters.InAngles(lifetimeArgs, typeOrConstArgs, assoc);
        }

        RsValueParameterList fnSugar = path.getValueParameterList();
        if (fnSugar != null) {
            List<RsTypeReference> inputArgs = new ArrayList<>();
            for (RsValueParameter parameter : fnSugar.getValueParameterList()) {
                inputArgs.add(parameter.getTypeReference());
            }
            RsRetType retType = path.getRetType();
            return new PathParameters.FnSugar(inputArgs, retType != null ? retType.getTypeReference() : null);
        }

        return null;
    }
}
