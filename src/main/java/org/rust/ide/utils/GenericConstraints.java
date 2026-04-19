/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.ide.utils;

import com.intellij.psi.PsiElement;
import com.intellij.psi.util.PsiTreeUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.rust.lang.core.psi.*;
import org.rust.lang.core.psi.ext.*;
import org.rust.lang.core.types.consts.Const;
import org.rust.lang.core.types.consts.CtConstParameter;
import org.rust.lang.core.types.infer.TypeVisitor;
import org.rust.lang.core.types.ty.Ty;
import org.rust.lang.core.types.ty.TyTypeParameter;

import java.util.*;
import java.util.stream.Collectors;
import org.rust.lang.core.psi.ext.RsTypeReferenceUtil;

public class GenericConstraints {
    @NotNull private final List<RsLifetimeParameter> myLifetimes;
    @NotNull private final List<RsTypeParameter> myTypeParameters;
    @NotNull private final List<RsConstParameter> myConstParameters;
    @NotNull private final List<RsWhereClause> myWhereClauses;

    public GenericConstraints(
        @NotNull List<RsLifetimeParameter> lifetimes,
        @NotNull List<RsTypeParameter> typeParameters,
        @NotNull List<RsConstParameter> constParameters,
        @NotNull List<RsWhereClause> whereClauses
    ) {
        this.myLifetimes = lifetimes;
        this.myTypeParameters = typeParameters;
        this.myConstParameters = constParameters;
        this.myWhereClauses = whereClauses;
    }

    public GenericConstraints() {
        this(Collections.emptyList(), Collections.emptyList(), Collections.emptyList(), Collections.emptyList());
    }

    @NotNull
    public List<RsLifetimeParameter> getLifetimes() {
        return myLifetimes;
    }

    @NotNull
    public List<RsTypeParameter> getTypeParameters() {
        return myTypeParameters;
    }

    @NotNull
    public List<RsConstParameter> getConstParameters() {
        return myConstParameters;
    }

    @NotNull
    public GenericConstraints filterByTypes(@NotNull List<Ty> types) {
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

    @NotNull
    public GenericConstraints filterByTypeReferences(@NotNull List<RsTypeReference> references) {
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

    @NotNull
    public String buildTypeParameters() {
        List<RsNameIdentifierOwner> all = new ArrayList<>();
        all.addAll(myLifetimes);
        all.addAll(myTypeParameters);
        all.addAll(myConstParameters);
        return joinToGenericListString(all);
    }

    @NotNull
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

    @NotNull
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

    @NotNull
    public GenericConstraints withoutTypes(@NotNull List<RsTypeParameter> params) {
        List<RsTypeParameter> types = new ArrayList<>(myTypeParameters);
        types.removeAll(params);
        return new GenericConstraints(myLifetimes, types, myConstParameters, myWhereClauses);
    }

    @NotNull
    public static GenericConstraints create(@NotNull PsiElement context) {
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

    @NotNull
    private static String joinToGenericListString(@NotNull List<? extends RsNameIdentifierOwner> items) {
        if (items.isEmpty()) return "";
        StringBuilder sb = new StringBuilder("<");
        for (int i = 0; i < items.size(); i++) {
            if (i > 0) sb.append(", ");
            sb.append(items.get(i).getText());
        }
        sb.append(">");
        return sb.toString();
    }

    @NotNull
    private static List<RsTypeParameter> gatherTypeParameters(@NotNull List<Ty> types, @NotNull List<RsTypeParameter> parameters) {
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

    @NotNull
    private static List<RsLifetimeParameter> gatherLifetimesFromTypeParameters(
        @NotNull List<RsLifetimeParameter> lifetimes,
        @NotNull List<RsTypeParameter> parameters
    ) {
        return gatherLifetimes(lifetimes, parameters, parameters);
    }

    @NotNull
    private static List<RsLifetimeParameter> gatherLifetimesFromTypeReferences(
        @NotNull List<RsTypeReference> references,
        @NotNull List<RsLifetimeParameter> lifetimes,
        @NotNull List<RsTypeParameter> parameters
    ) {
        return gatherLifetimes(lifetimes, parameters, references);
    }

    @NotNull
    private static List<RsLifetimeParameter> gatherLifetimes(
        @NotNull List<RsLifetimeParameter> lifetimes,
        @NotNull List<RsTypeParameter> parameters,
        @NotNull List<? extends PsiElement> elements
    ) {
        // Simplified - just return lifetimes present in the parameter list
        return new ArrayList<>(lifetimes);
    }

    private static boolean matchesConstParameter(@NotNull Ty type, @NotNull RsConstParameter parameter) {
        return type.visitWith(new TypeVisitor() {
            @Override
            public boolean visitConst(@NotNull Const c) {
                if (c instanceof CtConstParameter) {
                    return ((CtConstParameter) c).getParameter() == parameter;
                }
                return c.superVisitWith(this);
            }
        });
    }

    private static boolean hasTypeParameter(@NotNull RsTypeReference ref, @NotNull Map<String, RsTypeParameter> parameters) {
        Ty rawType = RsTypeReferenceUtil.getRawType(ref);
        return rawType.visitWith(new TypeVisitor() {
            @Override
            public boolean visitTy(@NotNull Ty ty) {
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

        CollectTypeParametersVisitor(@NotNull Map<String, RsTypeParameter> parameters, @NotNull Set<RsTypeParameter> collected) {
            this.myParameters = parameters;
            this.myCollected = collected;
        }

        @Override
        public boolean visitTy(@NotNull Ty ty) {
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
