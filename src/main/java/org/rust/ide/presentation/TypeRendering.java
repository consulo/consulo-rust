/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.ide.presentation;

import com.intellij.openapi.util.NlsSafe;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.rust.ide.utils.imports.ImportCandidatesCollector;
import org.rust.ide.utils.imports.ImportContext;
import org.rust.lang.core.psi.RsConstParameter;
import org.rust.lang.core.psi.RsLifetimeParameter;
import org.rust.lang.core.psi.RsTraitItem;
import org.rust.lang.core.psi.RsTypeParameter;
import org.rust.lang.core.psi.ext.*;
import org.rust.lang.core.types.BoundElement;
import org.rust.lang.core.types.Substitution;
import org.rust.lang.core.types.consts.Const;
import org.rust.lang.core.types.consts.CtConstParameter;
import org.rust.lang.core.types.consts.CtUnknown;
import org.rust.lang.core.types.consts.CtValue;
import org.rust.lang.core.types.regions.ReEarlyBound;
import org.rust.lang.core.types.regions.ReStatic;
import org.rust.lang.core.types.regions.ReUnknown;
import org.rust.lang.core.types.regions.Region;
import org.rust.lang.core.types.ty.*;
import org.rust.lang.core.types.ty.TyUtil;
import org.rust.lang.utils.evaluation.ConstExpr;
import org.rust.stdext.StdextUtil;

import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.rust.lang.core.psi.ext.RsTraitItemUtil;
import org.rust.lang.core.types.RsTypesUtil;
import org.rust.lang.utils.evaluation.ConstExprEvaluator;
import org.rust.lang.core.psi.ext.RsElement;
import org.rust.lang.core.psi.ext.RsGenericDeclaration;
import org.rust.lang.core.psi.RsTypeAlias;
import org.rust.lang.core.psi.ext.RsNamedElement;
import org.rust.lang.core.psi.ext.RsQualifiedNamedElement;

public final class TypeRendering {

    private static final int MAX_SHORT_TYPE_LEN = 50;

    private TypeRendering() {
    }

    @NlsSafe
    @NotNull
    public static String render(@NotNull Ty ty) {
        return render(ty, null, Integer.MAX_VALUE, "<unknown>", "<anonymous>",
            "'<unknown>", "<unknown>", "{integer}", "{float}",
            Collections.emptySet(), true, false, true, true);
    }

    @NlsSafe
    @NotNull
    public static String render(@NotNull Ty ty, boolean includeTypeArguments) {
        return render(ty, null, Integer.MAX_VALUE, "<unknown>", "<anonymous>",
            "'<unknown>", "<unknown>", "{integer}", "{float}",
            Collections.emptySet(), includeTypeArguments, false, true, true);
    }

    @NlsSafe
    @NotNull
    public static String render(@NotNull Ty ty, @NotNull Set<RsQualifiedNamedElement> useQualifiedName) {
        return render(ty, null, Integer.MAX_VALUE, "<unknown>", "<anonymous>",
            "'<unknown>", "<unknown>", "{integer}", "{float}",
            useQualifiedName, true, false, true, true);
    }

    @NlsSafe
    @NotNull
    public static String render(@NotNull Ty ty, @NotNull RsElement context, @NotNull Set<RsQualifiedNamedElement> useQualifiedName) {
        return render(ty, context, Integer.MAX_VALUE, "<unknown>", "<anonymous>",
            "'<unknown>", "<unknown>", "{integer}", "{float}",
            useQualifiedName, true, false, true, true);
    }

    @NlsSafe
    @NotNull
    public static String render(
        @NotNull Ty ty,
        @Nullable RsElement context,
        int level,
        @NotNull String unknown,
        @NotNull String anonymous,
        @NotNull String unknownLifetime,
        @NotNull String unknownConst,
        @NotNull String integer,
        @NotNull String floatStr,
        @NotNull Set<RsQualifiedNamedElement> useQualifiedName,
        boolean includeTypeArguments,
        boolean includeLifetimeArguments,
        boolean useAliasNames,
        boolean skipUnchangedDefaultGenericArguments
    ) {
        TypeRenderer renderer = new TypeRenderer(
            context, unknown, anonymous, unknownLifetime, unknownConst,
            integer, floatStr, useQualifiedName, includeTypeArguments,
            includeLifetimeArguments, useAliasNames, skipUnchangedDefaultGenericArguments
        );
        return renderer.render(ty, level);
    }

    @NotNull
    public static String renderInsertionSafe(@NotNull Ty ty) {
        return renderInsertionSafe(ty, null, Integer.MAX_VALUE,
            Collections.emptySet(), true, false, true, true);
    }

    @NotNull
    public static String renderInsertionSafe(
        @NotNull Ty ty,
        @Nullable RsElement context,
        int level,
        @NotNull Set<RsQualifiedNamedElement> useQualifiedName,
        boolean includeTypeArguments,
        boolean includeLifetimeArguments,
        boolean useAliasNames,
        boolean skipUnchangedDefaultGenericArguments
    ) {
        TypeRenderer renderer = new TypeRenderer(
            context, "_", "_", "'_", "{}", "_", "_",
            useQualifiedName, includeTypeArguments, includeLifetimeArguments,
            useAliasNames, skipUnchangedDefaultGenericArguments
        );
        return renderer.render(ty, level);
    }

    @NotNull
    public static String renderInsertionSafe(@NotNull Ty ty, @NotNull RsElement context) {
        return renderInsertionSafe(ty, context, Integer.MAX_VALUE,
            Collections.emptySet(), true, false, true, true);
    }

    @NotNull
    public static String renderInsertionSafe(@NotNull Ty ty, boolean includeTypeArguments, boolean includeLifetimeArguments) {
        return renderInsertionSafe(ty, null, Integer.MAX_VALUE,
            Collections.emptySet(), includeTypeArguments, includeLifetimeArguments, true, true);
    }

    @NotNull
    public static String renderInsertionSafe(@NotNull Ty ty, boolean includeTypeArguments, boolean includeLifetimeArguments, boolean useAliasNames) {
        return renderInsertionSafe(ty, null, Integer.MAX_VALUE,
            Collections.emptySet(), includeTypeArguments, includeLifetimeArguments, useAliasNames, true);
    }

    @NotNull
    public static String getShortPresentableText(@NotNull Ty ty) {
        String prev = null;
        for (int level = 1; ; level++) {
            String cur = render(ty, null, level, "?", "<anonymous>",
                "'<unknown>", "<unknown>", "{integer}", "{float}",
                Collections.emptySet(), true, false, true, true);
            if (cur.equals(prev) || (prev != null && cur.length() > MAX_SHORT_TYPE_LEN)) {
                break;
            }
            prev = cur;
        }
        return prev != null ? prev : "?";
    }

    private static final class TypeRenderer {
        final @Nullable RsElement context;
        final @NotNull String unknown;
        final @NotNull String anonymous;
        final @NotNull String unknownLifetime;
        final @NotNull String unknownConst;
        final @NotNull String integer;
        final @NotNull String floatStr;
        final @NotNull Set<RsQualifiedNamedElement> useQualifiedName;
        final boolean includeTypeArguments;
        final boolean includeLifetimeArguments;
        final boolean useAliasNames;
        final boolean skipUnchangedDefaultGenericArguments;

        TypeRenderer(
            @Nullable RsElement context,
            @NotNull String unknown,
            @NotNull String anonymous,
            @NotNull String unknownLifetime,
            @NotNull String unknownConst,
            @NotNull String integer,
            @NotNull String floatStr,
            @NotNull Set<RsQualifiedNamedElement> useQualifiedName,
            boolean includeTypeArguments,
            boolean includeLifetimeArguments,
            boolean useAliasNames,
            boolean skipUnchangedDefaultGenericArguments
        ) {
            this.context = context;
            this.unknown = unknown;
            this.anonymous = anonymous;
            this.unknownLifetime = unknownLifetime;
            this.unknownConst = unknownConst;
            this.integer = integer;
            this.floatStr = floatStr;
            this.useQualifiedName = useQualifiedName;
            this.includeTypeArguments = includeTypeArguments;
            this.includeLifetimeArguments = includeLifetimeArguments;
            this.useAliasNames = useAliasNames;
            this.skipUnchangedDefaultGenericArguments = skipUnchangedDefaultGenericArguments;
        }

        @NotNull
        String render(@NotNull Ty ty, int level) {
            if (level < 0) throw new IllegalArgumentException("level must be >= 0");
            if (ty instanceof TyUnknown) return unknown;
            if (level == 0) return "\u2026";

            Function<Ty, String> renderSub = subTy -> render(subTy, level - 1);

            BoundElement<?> aliasedBy = ty.getAliasedBy();
            if (useAliasNames && aliasedBy != null) {
                RsElement typedEl = (RsElement) aliasedBy.getElement();
                String elName = typedEl instanceof RsNamedElement ? getName((RsNamedElement) typedEl) : null;
                if (elName != null) {
                    List<String> visibleTypes = (typedEl instanceof RsGenericDeclaration)
                        ? formatGenerics((RsGenericDeclaration) typedEl, aliasedBy.getSubst(), renderSub)
                        : Collections.emptyList();
                    return elName + (visibleTypes.isEmpty() ? "" : "<" + String.join(", ", visibleTypes) + ">");
                }
                return anonymous;
            }

            if (ty instanceof TyPrimitive) {
                if (ty instanceof TyBool) return "bool";
                if (ty instanceof TyChar) return "char";
                if (ty instanceof TyUnit) return "()";
                if (ty instanceof TyNever) return "!";
                if (ty instanceof TyStr) return "str";
                if (ty instanceof TyInteger intTy) return intTy.getName();
                if (ty instanceof TyFloat floatTy) return floatTy.getName();
            }

            if (ty instanceof TyFunctionDef fnDef) {
                return formatFunctionDef(fnDef.getDef().getName(), fnDef.getUnsafety(), fnDef.getParamTypes(), fnDef.getRetType(), renderSub);
            }
            if (ty instanceof TyFunctionBase fnBase) {
                return formatFnLike("fn", fnBase.getUnsafety(), fnBase.getParamTypes(), fnBase.getRetType(), renderSub);
            }
            if (ty instanceof TySlice sliceTy) {
                return "[" + renderSub.apply(sliceTy.getElementType()) + "]";
            }
            if (ty instanceof TyTuple tupleTy) {
                List<Ty> types = tupleTy.getTypes();
                if (types.size() == 1) {
                    return "(" + renderSub.apply(types.get(0)) + ",)";
                }
                return types.stream().map(renderSub).collect(Collectors.joining(", ", "(", ")"));
            }
            if (ty instanceof TyArray arrayTy) {
                return "[" + renderSub.apply(arrayTy.getBase()) + "; " + renderConst(arrayTy.getConst()) + "]";
            }
            if (ty instanceof TyReference refTy) {
                StringBuilder sb = new StringBuilder("&");
                if (includeLifetimeArguments && (refTy.getRegion() instanceof ReEarlyBound || refTy.getRegion() instanceof ReStatic)) {
                    sb.append(renderRegion(refTy.getRegion()));
                    sb.append(" ");
                }
                if (refTy.getMutability().isMut()) sb.append("mut ");
                sb.append(render(refTy.getReferenced(), level));
                return sb.toString();
            }
            if (ty instanceof TyPointer ptrTy) {
                return "*" + (ptrTy.getMutability().isMut() ? "mut" : "const") + " " + renderSub.apply(ptrTy.getReferenced());
            }
            if (ty instanceof TyTypeParameter tyParam) {
                String name = tyParam.getParameter().toString();
                return name != null ? name : anonymous;
            }
            if (ty instanceof TyProjection projTy) {
                StringBuilder sb = new StringBuilder();
                String traitName = projTy.getTrait().getTypedElement().getName();
                if (traitName == null) return anonymous;
                if (TyUtil.isSelf(projTy.getType())) {
                    sb.append("Self::");
                } else {
                    sb.append("<").append(projTy.getType()).append(" as ").append(traitName);
                    if (includeTypeArguments) sb.append(formatTraitGenerics(projTy.getTrait(), renderSub, false));
                    sb.append(">::");
                }
                sb.append(projTy.getTarget().getTypedElement().getName());
                if (includeTypeArguments) sb.append(formatProjectionGenerics(projTy, renderSub));
                return sb.toString();
            }
            if (ty instanceof TyTraitObject traitObj) {
                return traitObj.getTraits().stream()
                    .map(t -> formatTrait(t, renderSub))
                    .collect(Collectors.joining("+", "dyn ", ""));
            }
            if (ty instanceof TyAnon anon) {
                return anon.getTraits().stream()
                    .map(t -> formatTrait(t, renderSub))
                    .collect(Collectors.joining("+", "impl ", ""));
            }
            if (ty instanceof TyAdt adt) {
                String adtName = getName(adt.getItem());
                if (adtName == null) return anonymous;
                return adtName + (includeTypeArguments ? formatAdtGenerics(adt, renderSub) : "");
            }
            if (ty instanceof TyInfer) {
                if (ty instanceof TyInfer.TyVar) return "_";
                if (ty instanceof TyInfer.IntVar) return integer;
                if (ty instanceof TyInfer.FloatVar) return floatStr;
            }
            if (ty instanceof TyPlaceholder) return "_";

            return unknown;
        }

        @NotNull
        private String renderRegion(@NotNull Region region) {
            return region instanceof ReUnknown ? unknownLifetime : region.toString();
        }

        @NotNull
        private String renderConst(@NotNull Const c) {
            return renderConst(c, false);
        }

        @NotNull
        private String renderConst(@NotNull Const c, boolean wrapParameterInBraces) {
            if (c instanceof CtValue) return c.toString();
            if (c instanceof CtConstParameter) return wrapParameterInBraces ? "{ " + c + " }" : c.toString();
            return unknownConst;
        }

        @NotNull
        private String formatFnLike(
            @NotNull String fnType,
            @NotNull Unsafety unsafety,
            @NotNull List<Ty> paramTypes,
            @NotNull Ty retType,
            @NotNull Function<Ty, String> renderSub
        ) {
            StringBuilder sb = new StringBuilder();
            buildFnLikeString(sb, fnType, unsafety, paramTypes, retType, renderSub);
            return sb.toString();
        }

        @NotNull
        private String formatFunctionDef(
            @Nullable String name,
            @NotNull Unsafety unsafety,
            @NotNull List<Ty> paramTypes,
            @NotNull Ty retType,
            @NotNull Function<Ty, String> renderSub
        ) {
            StringBuilder sb = new StringBuilder();
            buildFnLikeString(sb, "fn", unsafety, paramTypes, retType, renderSub);
            if (name != null) {
                sb.append(" {").append(name).append("}");
            }
            return sb.toString();
        }

        private void buildFnLikeString(
            @NotNull StringBuilder sb,
            @NotNull String fnType,
            @NotNull Unsafety unsafety,
            @NotNull List<Ty> paramTypes,
            @NotNull Ty retType,
            @NotNull Function<Ty, String> renderSub
        ) {
            String unsafetyPrefix = unsafety == Unsafety.Unsafe ? "unsafe " : "";
            sb.append(unsafetyPrefix).append(fnType).append("(");
            sb.append(paramTypes.stream().map(renderSub).collect(Collectors.joining(", ")));
            sb.append(")");
            if (!(retType instanceof TyUnit)) {
                sb.append(" -> ").append(renderSub.apply(retType));
            }
        }

        @NotNull
        private String formatTrait(@NotNull BoundElement<RsTraitItem> trait, @NotNull Function<Ty, String> renderSub) {
            String name = trait.getTypedElement().getName();
            if (name == null) return anonymous;

            String langAttr = RsTraitItemUtil.getLangAttribute(trait.getTypedElement());
            if (langAttr != null && (langAttr.equals("fn") || langAttr.equals("fn_once") || langAttr.equals("fn_mut"))) {
                List<RsTypeParameter> singleParam = trait.getTypedElement().getTypeParameters();
                if (singleParam.size() == 1) {
                    Ty paramTy = trait.getSubst().get(singleParam.get(0));
                    if (paramTy instanceof TyTuple tupleTy) {
                        List<Ty> paramTypes = tupleTy.getTypes();
                        Ty retType = TyUnit.INSTANCE;
                        for (var entry : trait.getAssoc().entrySet()) {
                            if ("Output".equals(entry.getKey().getName())) {
                                retType = entry.getValue();
                                break;
                            }
                        }
                        return formatFnLike(name, Unsafety.fromBoolean(trait.getTypedElement().isUnsafe()), paramTypes, retType, renderSub);
                    }
                }
                return unknown;
            }

            return name + (includeTypeArguments ? formatTraitGenerics(trait, renderSub) : "");
        }

        @NotNull
        private String formatAdtGenerics(@NotNull TyAdt adt, @NotNull Function<Ty, String> renderSub) {
            List<String> visibleTypes = formatGenerics(adt.getItem(), adt.getTypeParameterValues(), renderSub);
            return visibleTypes.isEmpty() ? "" : "<" + String.join(", ", visibleTypes) + ">";
        }

        @NotNull
        private String formatProjectionGenerics(@NotNull TyProjection projection, @NotNull Function<Ty, String> renderSub) {
            List<String> visibleTypes = formatGenerics(projection.getTarget().getTypedElement(), projection.getTypeParameterValues(), renderSub);
            return visibleTypes.isEmpty() ? "" : "<" + String.join(", ", visibleTypes) + ">";
        }

        @NotNull
        private String formatTraitGenerics(@NotNull BoundElement<RsTraitItem> trait, @NotNull Function<Ty, String> renderSub) {
            return formatTraitGenerics(trait, renderSub, true);
        }

        @NotNull
        private String formatTraitGenerics(@NotNull BoundElement<RsTraitItem> trait, @NotNull Function<Ty, String> renderSub, boolean includeAssoc) {
            List<String> assoc;
            if (includeAssoc) {
                assoc = new ArrayList<>();
                for (RsTypeAlias it : RsTraitItemUtil.getAssociatedTypesTransitively(trait.getTypedElement())) {
                    String itName = it.getName();
                    if (itName == null) continue;
                    Ty val = trait.getAssoc().get(it);
                    assoc.add(itName + "=" + renderSub.apply(val != null ? val : TyUnknown.INSTANCE));
                }
            } else {
                assoc = Collections.emptyList();
            }
            List<String> visibleTypes = new ArrayList<>(formatBoundElementGenerics(trait, renderSub));
            visibleTypes.addAll(assoc);
            return visibleTypes.isEmpty() ? "" : "<" + String.join(", ", visibleTypes) + ">";
        }

        @NotNull
        private <T extends RsGenericDeclaration & RsNamedElement> String formatBoundElement(
            @NotNull BoundElement<T> boundElement,
            @NotNull Function<Ty, String> renderSub
        ) {
            String elName = getName(boundElement.getTypedElement());
            if (elName == null) return anonymous;
            List<String> visibleTypes = formatBoundElementGenerics(boundElement, renderSub);
            return elName + (visibleTypes.isEmpty() ? "" : "<" + String.join(", ", visibleTypes) + ">");
        }

        @NotNull
        private List<String> formatBoundElementGenerics(
            @NotNull BoundElement<? extends RsGenericDeclaration> boundElement,
            @NotNull Function<Ty, String> renderSub
        ) {
            return formatGenerics(boundElement.getTypedElement(), boundElement.getSubst(), renderSub);
        }

        @NotNull
        private List<String> formatGenerics(
            @NotNull RsGenericDeclaration declaration,
            @NotNull Substitution subst,
            @NotNull Function<Ty, String> renderSub
        ) {
            List<String> renderedList = new ArrayList<>();
            boolean nonDefaultParamFound = false;
            List<? extends RsElement> params = RsGenericDeclarationUtil.getGenericParameters(declaration);
            List<? extends RsElement> reversed = new ArrayList<>(params);
            Collections.reverse(reversed);

            for (RsElement parameter : reversed) {
                if (skipUnchangedDefaultGenericArguments && !nonDefaultParamFound) {
                    if (parameter instanceof RsTypeParameter tp) {
                        if (tp.getTypeReference() != null) {
                            Ty normType = org.rust.lang.core.types.RsTypesUtil.getNormType(tp.getTypeReference());
                            Ty substTy = subst.get(tp);
                            if (normType != null && substTy != null && normType.isEquivalentTo(substTy)) {
                                continue;
                            }
                        }
                    } else if (parameter instanceof RsConstParameter cp) {
                        if (cp.getExpr() != null) {
                            Ty expectedTy = cp.getTypeReference() != null
                                ? org.rust.lang.core.types.RsTypesUtil.getNormType(cp.getTypeReference())
                                : TyUnknown.INSTANCE;
                            Const evaluated = ConstExprEvaluator.evaluate(cp.getExpr(), expectedTy != null ? expectedTy : TyUnknown.INSTANCE);
                            if (evaluated != null && evaluated.equals(subst.get(cp))) {
                                continue;
                            }
                        }
                    }
                    nonDefaultParamFound = true;
                }

                String rendered;
                if (parameter instanceof RsLifetimeParameter lp) {
                    if (!includeLifetimeArguments) continue;
                    Region r = subst.get(lp);
                    rendered = renderRegion(r != null ? r : ReUnknown.INSTANCE);
                } else if (parameter instanceof RsTypeParameter tp) {
                    Ty t = subst.get(tp);
                    rendered = renderSub.apply(t != null ? t : TyUnknown.INSTANCE);
                } else if (parameter instanceof RsConstParameter cp) {
                    Const c = subst.get(cp);
                    rendered = renderConst(c != null ? c : CtUnknown.INSTANCE, true);
                } else {
                    throw new IllegalStateException("unreachable");
                }
                renderedList.add(rendered);
            }
            Collections.reverse(renderedList);
            return renderedList;
        }

        @Nullable
        private String getName(@NotNull RsNamedElement element) {
            if (element instanceof RsQualifiedNamedElement qElement && useQualifiedName.contains(qElement)) {
                if (context != null) {
                    ImportContext importContext = ImportContext.from(context, ImportContext.Type.OTHER);
                    if (importContext != null) {
                        var candidate = ImportCandidatesCollector.findImportCandidate(importContext, qElement);
                        if (candidate != null) {
                            return candidate.getInfo().getUsePath();
                        }
                    }
                }
                return qElement.qualifiedName();
            }
            return element.getName();
        }
    }
}
