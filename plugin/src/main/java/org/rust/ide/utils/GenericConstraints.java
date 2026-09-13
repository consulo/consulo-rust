/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.ide.utils;

import consulo.language.psi.PsiElement;
import consulo.language.psi.util.PsiTreeUtil;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import org.rust.lang.core.psi.*;
import org.rust.lang.core.psi.ext.*;
import org.rust.lang.core.types.consts.Const;
import org.rust.lang.core.types.consts.CtConstParameter;
import org.rust.lang.core.types.infer.TypeVisitor;
import org.rust.lang.core.types.ty.Ty;
import org.rust.lang.core.types.ty.TyTypeParameter;

import java.util.*;
import java.util.stream.Collectors;
import org.rust.lang.core.psi.ext.impl.RsTypeReferenceUtil;
import org.rust.lang.core.psi.ext.impl.*;

public class GenericConstraints {
    @Nonnull private final List<RsLifetimeParameter> myLifetimes;
    @Nonnull private final List<RsTypeParameter> myTypeParameters;
    @Nonnull private final List<RsConstParameter> myConstParameters;
    @Nonnull private final List<RsWhereClause> myWhereClauses;

    public GenericConstraints(
        @Nonnull List<RsLifetimeParameter> lifetimes,
        @Nonnull List<RsTypeParameter> typeParameters,
        @Nonnull List<RsConstParameter> constParameters,
        @Nonnull List<RsWhereClause> whereClauses
    ) {
        this.myLifetimes = lifetimes;
        this.myTypeParameters = typeParameters;
        this.myConstParameters = constParameters;
        this.myWhereClauses = whereClauses;
    }

    public GenericConstraints() {
        this(Collections.emptyList(), Collections.emptyList(), Collections.emptyList(), Collections.emptyList());
    }

    @Nonnull
    public List<RsLifetimeParameter> getLifetimes() {
        return myLifetimes;
    }

    @Nonnull
    public List<RsTypeParameter> getTypeParameters() {
        return myTypeParameters;
    }

    @Nonnull
    public List<RsConstParameter> getConstParameters() {
        return myConstParameters;
    }

    @Nonnull
    public GenericConstraints filterByTypes(@Nonnull List<Ty> types) {
        List<RsTypeParameter> typeParameters = gatherTypeParameters(types, myTypeParameters);
        List<RsConstParameter> constParameters = new ArrayList<>();
        for (RsConstParameter param : myConstParameters) {
            for (Ty type : types) {
                if (matchesConstParameter(type, param)) {
                    constParameters.add(param);
                    break;
                }
            }
        }
        List<RsLifetimeParameter> lifetimes = gatherLifetimesFromTypeParameters(myLifetimes, typeParameters);
        return new GenericConstraints(lifetimes, typeParameters, constParameters, myWhereClauses);
    }

    @Nonnull
    public GenericConstraints filterByTypeReferences(@Nonnull List<RsTypeReference> references) {
        List<Ty> types = new ArrayList<>();
        for (RsTypeReference ref : references) {
            types.add(RsTypeReferenceUtil.getRawType(ref));
        }
        List<RsTypeParameter> typeParameters = gatherTypeParameters(types, myTypeParameters);
        List<RsLifetimeParameter> lifetimes = gatherLifetimesFromTypeReferences(references, myLifetimes, typeParameters);
        List<RsConstParameter> constParameters = new ArrayList<>();
        for (RsConstParameter param : myConstParameters) {
            for (Ty type : types) {
                if (matchesConstParameter(type, param)) {
                    constParameters.add(param);
                    break;
                }
            }
        }
        return new GenericConstraints(lifetimes, typeParameters, constParameters, myWhereClauses);
    }

    @Nonnull
    public String buildTypeParameters() {
        List<RsNameIdentifierOwner> all = new ArrayList<>();
        all.addAll(myLifetimes);
        all.addAll(myTypeParameters);
        all.addAll(myConstParameters);
        return joinToGenericListString(all);
    }

    @Nonnull
    public String buildTypeArguments() {
        List<RsNameIdentifierOwner> all = new ArrayList<>();
        all.addAll(myLifetimes);
        all.addAll(myTypeParameters);
        all.addAll(myConstParameters);
        if (all.isEmpty()) return "";
        StringBuilder sb = new StringBuilder("<");
        for (int i = 0; i < all.size(); i++) {
            if (i > 0) sb.append(", ");
            String name = all.get(i).getName();
            sb.append(name != null ? name : "");
        }
        sb.append(">");
        return sb.toString();
    }

    @Nonnull
    public String buildWhereClause() {
        List<RsWherePred> wherePredList = new ArrayList<>();
        for (RsWhereClause clause : myWhereClauses) {
            wherePredList.addAll(clause.getWherePredList());
        }

        Map<String, RsTypeParameter> parameterMap = new HashMap<>();
        for (RsTypeParameter param : myTypeParameters) {
            if (param.getName() != null) {
                parameterMap.put(param.getName(), param);
            }
        }
        Map<String, RsLifetimeParameter> lifetimeMap = new HashMap<>();
        for (RsLifetimeParameter lt : myLifetimes) {
            if (lt.getName() != null) {
                lifetimeMap.put(lt.getName(), lt);
            }
        }
        Map<String, Set<String>> parameterToBounds = new LinkedHashMap<>();
        Map<String, Set<String>> lifetimeToBounds = new LinkedHashMap<>();

        for (RsWherePred predicate : wherePredList) {
            RsTypeReference typeRef = predicate.getTypeReference();
            RsLifetime lifetime = predicate.getLifetime();
            if (typeRef != null && hasTypeParameter(typeRef, parameterMap)) {
                String forLifetimes = predicate.getForLifetimes() != null ? predicate.getForLifetimes().getText() + " " : "";
                String parameterText = forLifetimes + typeRef.getText();
                parameterToBounds.computeIfAbsent(parameterText, k -> new TreeSet<>());
                if (predicate.getTypeParamBounds() != null) {
                    for (RsPolybound bound : predicate.getTypeParamBounds().getPolyboundList()) {
                        parameterToBounds.get(parameterText).add(bound.getText());
                    }
                }
            } else if (lifetime != null && lifetime.getName() != null && lifetimeMap.containsKey(lifetime.getName())) {
                lifetimeToBounds.computeIfAbsent(lifetime.getName(), k -> new TreeSet<>());
                if (predicate.getLifetimeParamBounds() != null) {
                    for (RsLifetime bound : predicate.getLifetimeParamBounds().getLifetimeList()) {
                        if (lifetimeMap.containsKey(bound.getName())) {
                            lifetimeToBounds.get(lifetime.getName()).add(bound.getText());
                        }
                    }
                }
            }
        }

        List<String> bounds = new ArrayList<>();
        for (Map.Entry<String, Set<String>> entry : lifetimeToBounds.entrySet()) {
            if (!entry.getValue().isEmpty()) {
                bounds.add(entry.getKey() + ": " + String.join(" + ", entry.getValue()));
            }
        }
        for (Map.Entry<String, Set<String>> entry : parameterToBounds.entrySet()) {
            if (!entry.getValue().isEmpty()) {
                bounds.add(entry.getKey() + ": " + String.join(" + ", entry.getValue()));
            }
        }

        if (bounds.isEmpty()) return "";
        return " where " + String.join(",", bounds);
    }

    @Nonnull
    public GenericConstraints withoutTypes(@Nonnull List<RsTypeParameter> params) {
        List<RsTypeParameter> types = new ArrayList<>(myTypeParameters);
        types.removeAll(params);
        return new GenericConstraints(myLifetimes, types, myConstParameters, myWhereClauses);
    }

    @Nonnull
    public static GenericConstraints create(@Nonnull PsiElement context) {
        Set<RsTypeParameter> typeParameters = new LinkedHashSet<>();
        Set<RsLifetimeParameter> lifetimes = new LinkedHashSet<>();
        Set<RsConstParameter> constParameters = new LinkedHashSet<>();
        List<RsWhereClause> whereClauses = new ArrayList<>();

        RsGenericDeclaration genericDecl = context instanceof RsGenericDeclaration
            ? (RsGenericDeclaration) context
            : PsiTreeUtil.getContextOfType(context, RsGenericDeclaration.class);

        while (genericDecl != null) {
            typeParameters.addAll(genericDecl.getTypeParameters());
            lifetimes.addAll(genericDecl.getLifetimeParameters());
            constParameters.addAll(genericDecl.getConstParameters());
            if (genericDecl.getWhereClause() != null) {
                whereClauses.add(genericDecl.getWhereClause());
            }

            if (genericDecl instanceof RsAbstractable) {
                RsAbstractableOwner owner = RsAbstractableUtil.getOwner((RsAbstractable) genericDecl);
                if (!(owner instanceof RsAbstractableOwner.Impl || owner instanceof RsAbstractableOwner.Trait)) {
                    break;
                }
            }

            genericDecl = PsiTreeUtil.getContextOfType(genericDecl, RsGenericDeclaration.class);
        }

        return new GenericConstraints(
            new ArrayList<>(lifetimes),
            new ArrayList<>(typeParameters),
            new ArrayList<>(constParameters),
            whereClauses
        );
    }

    @Nonnull
    private static String joinToGenericListString(@Nonnull List<? extends RsNameIdentifierOwner> items) {
        if (items.isEmpty()) return "";
        StringBuilder sb = new StringBuilder("<");
        for (int i = 0; i < items.size(); i++) {
            if (i > 0) sb.append(", ");
            sb.append(items.get(i).getText());
        }
        sb.append(">");
        return sb.toString();
    }

    @Nonnull
    private static List<RsTypeParameter> gatherTypeParameters(@Nonnull List<Ty> types, @Nonnull List<RsTypeParameter> parameters) {
        Map<String, RsTypeParameter> parameterMap = new HashMap<>();
        for (RsTypeParameter param : parameters) {
            if (param.getName() != null) {
                parameterMap.put(param.getName(), param);
            }
        }
        Set<RsTypeParameter> collected = new LinkedHashSet<>();
        for (Ty type : types) {
            type.visitWith(new CollectTypeParametersVisitor(parameterMap, collected));
        }
        List<RsTypeParameter> result = new ArrayList<>(collected);
        result.sort(Comparator.comparingInt(parameters::indexOf));
        return result;
    }

    @Nonnull
    private static List<RsLifetimeParameter> gatherLifetimesFromTypeParameters(
        @Nonnull List<RsLifetimeParameter> lifetimes,
        @Nonnull List<RsTypeParameter> parameters
    ) {
        return gatherLifetimes(lifetimes, parameters, parameters);
    }

    @Nonnull
    private static List<RsLifetimeParameter> gatherLifetimesFromTypeReferences(
        @Nonnull List<RsTypeReference> references,
        @Nonnull List<RsLifetimeParameter> lifetimes,
        @Nonnull List<RsTypeParameter> parameters
    ) {
        return gatherLifetimes(lifetimes, parameters, references);
    }

    @Nonnull
    private static List<RsLifetimeParameter> gatherLifetimes(
        @Nonnull List<RsLifetimeParameter> lifetimes,
        @Nonnull List<RsTypeParameter> parameters,
        @Nonnull List<? extends PsiElement> elements
    ) {
        // Simplified - just return lifetimes present in the parameter list
        return new ArrayList<>(lifetimes);
    }

    private static boolean matchesConstParameter(@Nonnull Ty type, @Nonnull RsConstParameter parameter) {
        return type.visitWith(new TypeVisitor() {
            @Override
            public boolean visitConst(@Nonnull Const c) {
                if (c instanceof CtConstParameter) {
                    return ((CtConstParameter) c).getParameter() == parameter;
                }
                return c.superVisitWith(this);
            }
        });
    }

    private static boolean hasTypeParameter(@Nonnull RsTypeReference ref, @Nonnull Map<String, RsTypeParameter> parameters) {
        Ty rawType = RsTypeReferenceUtil.getRawType(ref);
        return rawType.visitWith(new TypeVisitor() {
            @Override
            public boolean visitTy(@Nonnull Ty ty) {
                if (ty instanceof TyTypeParameter) {
                    return parameters.containsKey(ty.toString());
                }
                return ty.superVisitWith(this);
            }
        });
    }

    private static class CollectTypeParametersVisitor implements TypeVisitor {
        private final Map<String, RsTypeParameter> myParameters;
        private final Set<RsTypeParameter> myCollected;

        CollectTypeParametersVisitor(@Nonnull Map<String, RsTypeParameter> parameters, @Nonnull Set<RsTypeParameter> collected) {
            this.myParameters = parameters;
            this.myCollected = collected;
        }

        @Override
        public boolean visitTy(@Nonnull Ty ty) {
            if (ty instanceof TyTypeParameter) {
                RsTypeParameter parameter = myParameters.get(ty.toString());
                if (parameter != null) {
                    myCollected.add(parameter);
                }
                return false;
            }
            return ty.superVisitWith(this);
        }
    }
}
