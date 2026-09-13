/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.ide.hints.type;

import consulo.language.editor.inlay.InlayPresentation;
import com.intellij.codeInsight.hints.presentation.PresentationFactory;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import org.rust.lang.core.presentation.TypeRendering;
import org.rust.lang.core.psi.RsConstParameter;
import org.rust.lang.core.psi.RsTraitItem;
import org.rust.lang.core.psi.RsTypeAlias;
import org.rust.lang.core.psi.RsTypeParameter;
import org.rust.lang.core.psi.ext.impl.RsGenericDeclarationUtil;
import org.rust.lang.core.types.BoundElement;
import org.rust.lang.core.types.Kind;
import org.rust.lang.core.types.consts.Const;
import org.rust.lang.core.types.consts.CtConstParameter;
import org.rust.lang.core.types.consts.CtUnknown;
import org.rust.lang.core.types.consts.CtValue;
import org.rust.lang.core.types.ty.*;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import org.rust.lang.core.types.RsTypesUtil;
import org.rust.lang.utils.evaluation.ConstExprEvaluator;

@SuppressWarnings("UnstableApiUsage")
public class RsTypeHintsPresentationFactory {

    private static final String PLACEHOLDER = "\u2026";
    private static final int FOLDING_THRESHOLD = 3;

    private final PresentationFactory myFactory;
    private final boolean myShowObviousTypes;

    public RsTypeHintsPresentationFactory(@Nonnull PresentationFactory factory, boolean showObviousTypes) {
        myFactory = factory;
        myShowObviousTypes = showObviousTypes;
    }

    @Nonnull
    public InlayPresentation typeHint(@Nonnull Ty type) {
        InlayPresentation colon = text(": ");
        InlayPresentation typePresentation = hint(type, 1);
        return myFactory.roundWithBackground(join(List.of(colon, typePresentation), ""));
    }

    @Nonnull
    private InlayPresentation hint(@Nonnull Kind kind, int level) {
        if (kind instanceof Ty) {
            BoundElement<RsTypeAlias> alias = ((Ty) kind).getAliasedBy();
            if (alias != null) {
                return aliasTypeHint(alias, level);
            }
        }

        if (kind instanceof TyTuple) return tupleTypeHint((TyTuple) kind, level);
        if (kind instanceof TyAdt) return adtTypeHint((TyAdt) kind, level);
        if (kind instanceof TyFunctionBase) return functionTypeHint((TyFunctionBase) kind, level);
        if (kind instanceof TyReference) return referenceTypeHint((TyReference) kind, level);
        if (kind instanceof TyPointer) return pointerTypeHint((TyPointer) kind, level);
        if (kind instanceof TyProjection) return projectionTypeHint((TyProjection) kind, level);
        if (kind instanceof TyTypeParameter) return typeParameterTypeHint((TyTypeParameter) kind);
        if (kind instanceof TyArray) return arrayTypeHint((TyArray) kind, level);
        if (kind instanceof TySlice) return sliceTypeHint((TySlice) kind, level);
        if (kind instanceof TyTraitObject) return traitObjectTypeHint((TyTraitObject) kind, level);
        if (kind instanceof TyAnon) return anonTypeHint((TyAnon) kind, level);
        if (kind instanceof CtConstParameter) return constParameterTypeHint((CtConstParameter) kind);
        if (kind instanceof CtValue) return text(kind.toString());
        if (kind instanceof Ty) return text(TypeRendering.getShortPresentableText((Ty) kind));
        return text("?");
    }

    @Nonnull
    private InlayPresentation functionTypeHint(@Nonnull TyFunctionBase type, int level) {
        List<Ty> parameters = type.getParamTypes();
        Ty returnType = type.getRetType();

        boolean startWithPlaceholder = checkSize(level, parameters.size() + 1);
        InlayPresentation fn;
        if (parameters.isEmpty()) {
            fn = text("fn()");
        } else {
            fn = myFactory.collapsible(
                text("fn("), text(PLACEHOLDER),
                () -> parametersHint(new ArrayList<>(parameters), level + 1),
                text(")"), startWithPlaceholder
            );
        }

        if (!(returnType instanceof TyUnit)) {
            InlayPresentation ret = myFactory.collapsible(
                text(" \u2192 "), text(PLACEHOLDER),
                () -> hint(returnType, level + 1),
                text(""), startWithPlaceholder
            );
            return myFactory.seq(fn, ret);
        }

        return fn;
    }

    @Nonnull
    private InlayPresentation tupleTypeHint(@Nonnull TyTuple type, int level) {
        return myFactory.collapsible(
            text("("), text(PLACEHOLDER),
            () -> tupleTypesHint(type.getTypes(), level + 1),
            text(")"), checkSize(level, type.getTypes().size())
        );
    }

    @Nonnull
    private InlayPresentation tupleTypesHint(@Nonnull List<Ty> types, int level) {
        if (types.size() == 1) {
            return myFactory.seq(hint(types.get(0), level), text(","));
        }
        List<InlayPresentation> presentations = new ArrayList<>();
        for (Ty ty : types) {
            presentations.add(hint(ty, level));
        }
        return join(presentations, ", ");
    }

    @Nonnull
    private InlayPresentation adtTypeHint(@Nonnull TyAdt type, int level) {
        String adtName = type.getItem().getName();
        InlayPresentation typeNamePresentation = myFactory.psiSingleReference(text(adtName), () -> type.getItem());
        List<Pair<Ty, RsTypeParameter>> typeArguments = zipTypeArgs(type.getTypeArguments(), RsGenericDeclarationUtil.getTypeParameters(type.getItem()));
        List<Pair<Const, RsConstParameter>> constArguments = zipConstArgs(type.getConstArguments(), RsGenericDeclarationUtil.getConstParameters(type.getItem()));
        return withGenericsTypeHint(typeNamePresentation, typeArguments, constArguments, level);
    }

    @Nonnull
    private InlayPresentation aliasTypeHint(@Nonnull BoundElement<RsTypeAlias> boundElement, int level) {
        RsTypeAlias alias = boundElement.getTypedElement();
        String adtName = alias.getName();
        InlayPresentation typeNamePresentation = myFactory.psiSingleReference(text(adtName), () -> alias);
        List<Pair<Ty, RsTypeParameter>> typeArguments = new ArrayList<>();
        for (RsTypeParameter param : RsGenericDeclarationUtil.getTypeParameters(alias)) {
            Ty ty = boundElement.getSubst().get(param);
            typeArguments.add(new Pair<>(ty != null ? ty : TyUnknown.INSTANCE, param));
        }
        List<Pair<Const, RsConstParameter>> constArguments = new ArrayList<>();
        for (RsConstParameter param : RsGenericDeclarationUtil.getConstParameters(alias)) {
            Const c = boundElement.getSubst().get(param);
            constArguments.add(new Pair<>(c != null ? c : CtUnknown.INSTANCE, param));
        }
        return withGenericsTypeHint(typeNamePresentation, typeArguments, constArguments, level);
    }

    @Nonnull
    private InlayPresentation projectionTypeHint(@Nonnull TyProjection type, int level) {
        InlayPresentation collapsible = myFactory.collapsible(
            text("<"), text(PLACEHOLDER),
            () -> {
                InlayPresentation typePresentation = hint(type.getType(), level + 1);
                InlayPresentation traitPresentation = traitItemTypeHint(type.getTrait(), level + 1, false);
                return join(List.of(typePresentation, traitPresentation), " as ");
            },
            text(">"), checkSize(level, 2)
        );

        RsTypeAlias target = type.getTarget().getTypedElement();
        String targetName = target.getName();
        InlayPresentation targetPresentation = myFactory.psiSingleReference(text(targetName), () -> target);
        InlayPresentation typeNamePresentation = join(List.of(collapsible, targetPresentation), "::");
        List<Pair<Ty, RsTypeParameter>> typeArguments = zipTypeArgs(BoundElement.positionalTypeArguments(type.getTarget()), RsGenericDeclarationUtil.getTypeParameters(target));
        return withGenericsTypeHint(typeNamePresentation, typeArguments, Collections.emptyList(), level);
    }

    @Nonnull
    private InlayPresentation withGenericsTypeHint(
        @Nonnull InlayPresentation typeNamePresentation,
        @Nonnull List<Pair<Ty, RsTypeParameter>> typeArguments,
        @Nonnull List<Pair<Const, RsConstParameter>> constArguments,
        int level
    ) {
        List<Kind> userVisibleKindArguments = new ArrayList<>();
        // Merge and sort by offset
        List<Object[]> all = new ArrayList<>();
        for (Pair<Ty, RsTypeParameter> pair : typeArguments) {
            all.add(new Object[]{pair.myFirst, pair.mySecond, pair.mySecond.getTextOffset()});
        }
        for (Pair<Const, RsConstParameter> pair : constArguments) {
            all.add(new Object[]{pair.myFirst, pair.mySecond, pair.mySecond.getTextOffset()});
        }
        all.sort((a, b) -> Integer.compare((int) a[2], (int) b[2]));

        for (Object[] entry : all) {
            Kind argument = (Kind) entry[0];
            if (!myShowObviousTypes) {
                if (argument instanceof Ty && entry[1] instanceof RsTypeParameter) {
                    if (isDefaultTypeParameter((Ty) argument, (RsTypeParameter) entry[1])) continue;
                }
                if (argument instanceof Const && entry[1] instanceof RsConstParameter) {
                    if (isDefaultConstParameter((Const) argument, (RsConstParameter) entry[1])) continue;
                }
            }
            userVisibleKindArguments.add(argument);
        }

        if (!userVisibleKindArguments.isEmpty()) {
            InlayPresentation collapsible = myFactory.collapsible(
                text("<"), text(PLACEHOLDER),
                () -> parametersHint(userVisibleKindArguments, level + 1),
                text(">"), checkSize(level, userVisibleKindArguments.size())
            );
            return join(List.of(typeNamePresentation, collapsible), "");
        }

        return typeNamePresentation;
    }

    @Nonnull
    private InlayPresentation referenceTypeHint(@Nonnull TyReference type, int level) {
        String prefix = "&" + (type.getMutability().isMut() ? "mut " : "");
        return join(List.of(text(prefix), hint(type.getReferenced(), level)), "");
    }

    @Nonnull
    private InlayPresentation pointerTypeHint(@Nonnull TyPointer type, int level) {
        String prefix = "*" + (type.getMutability().isMut() ? "mut " : "const ");
        return join(List.of(text(prefix), hint(type.getReferenced(), level)), "");
    }

    @Nonnull
    private InlayPresentation typeParameterTypeHint(@Nonnull TyTypeParameter type) {
        TyTypeParameter.TypeParameter parameter = type.getParameter();
        if (parameter instanceof TyTypeParameter.Named) {
            TyTypeParameter.Named named = (TyTypeParameter.Named) parameter;
            return myFactory.psiSingleReference(text(named.getParameter().getName()), () -> named.getParameter());
        }
        return text(type.toString());
    }

    @Nonnull
    private InlayPresentation constParameterTypeHint(@Nonnull CtConstParameter constParam) {
        return myFactory.psiSingleReference(text(constParam.getParameter().getName()), () -> constParam.getParameter());
    }

    @Nonnull
    private InlayPresentation arrayTypeHint(@Nonnull TyArray type, int level) {
        return myFactory.collapsible(
            text("["), text(PLACEHOLDER),
            () -> {
                InlayPresentation basePresentation = hint(type.getBase(), level + 1);
                InlayPresentation sizePresentation = text(type.getSize() != null ? type.getSize().toString() : "?");
                return join(List.of(basePresentation, sizePresentation), "; ");
            },
            text("]"), checkSize(level, 1)
        );
    }

    @Nonnull
    private InlayPresentation sliceTypeHint(@Nonnull TySlice type, int level) {
        return myFactory.collapsible(
            text("["), text(PLACEHOLDER),
            () -> hint(type.getElementType(), level + 1),
            text("]"), checkSize(level, 1)
        );
    }

    @Nonnull
    private InlayPresentation traitObjectTypeHint(@Nonnull TyTraitObject type, int level) {
        return myFactory.collapsible(
            text("dyn "), text(PLACEHOLDER),
            () -> {
                List<InlayPresentation> presentations = new ArrayList<>();
                for (BoundElement<RsTraitItem> trait : type.getTraits()) {
                    presentations.add(traitItemTypeHint(trait, level + 1, true));
                }
                return join(presentations, "+");
            },
            text(""), checkSize(level, 1)
        );
    }

    @Nonnull
    private InlayPresentation anonTypeHint(@Nonnull TyAnon type, int level) {
        return myFactory.collapsible(
            text("impl "), text(PLACEHOLDER),
            () -> {
                List<InlayPresentation> presentations = new ArrayList<>();
                for (BoundElement<RsTraitItem> trait : type.getTraits()) {
                    presentations.add(traitItemTypeHint(trait, level + 1, true));
                }
                return join(presentations, "+");
            },
            text(""), checkSize(level, type.getTraits().size())
        );
    }

    @Nonnull
    private InlayPresentation parametersHint(@Nonnull List<? extends Kind> kinds, int level) {
        List<InlayPresentation> presentations = new ArrayList<>();
        for (Kind kind : kinds) {
            presentations.add(hint(kind, level));
        }
        return join(presentations, ", ");
    }

    @Nonnull
    private InlayPresentation traitItemTypeHint(@Nonnull BoundElement<RsTraitItem> trait, int level, boolean includeAssoc) {
        InlayPresentation traitPresentation = myFactory.psiSingleReference(text(trait.getTypedElement().getName()), () -> trait.getTypedElement());

        List<InlayPresentation> innerPresentations = new ArrayList<>();

        List<RsTypeParameter> typeParams = RsGenericDeclarationUtil.getTypeParameters(trait.getTypedElement());
        List<RsConstParameter> constParams = RsGenericDeclarationUtil.getConstParameters(trait.getTypedElement());

        // Sort by offset
        List<Object[]> genericParams = new ArrayList<>();
        for (RsTypeParameter tp : typeParams) {
            genericParams.add(new Object[]{tp, tp.getTextOffset()});
        }
        for (RsConstParameter cp : constParams) {
            genericParams.add(new Object[]{cp, cp.getTextOffset()});
        }
        genericParams.sort((a, b) -> Integer.compare((int) a[1], (int) b[1]));

        for (Object[] entry : genericParams) {
            if (entry[0] instanceof RsTypeParameter) {
                RsTypeParameter parameter = (RsTypeParameter) entry[0];
                Ty argument = trait.getSubst().get(parameter);
                if (argument == null) continue;
                if (!myShowObviousTypes && isDefaultTypeParameter(argument, parameter)) continue;
                innerPresentations.add(hint(argument, level + 1));
            } else if (entry[0] instanceof RsConstParameter) {
                RsConstParameter parameter = (RsConstParameter) entry[0];
                Const argument = trait.getSubst().get(parameter);
                if (argument == null) continue;
                if (!myShowObviousTypes && isDefaultConstParameter(argument, parameter)) continue;
                innerPresentations.add(hint(argument, level + 1));
            }
        }

        if (includeAssoc) {
            for (RsTypeAlias alias : trait.getTypedElement().getAssociatedTypesTransitively()) {
                String aliasName = alias.getName();
                if (aliasName == null) continue;
                Ty type = trait.getAssoc().get(alias);
                if (type == null) continue;
                if (!myShowObviousTypes && isDefaultTypeAlias(type, alias)) continue;
                InlayPresentation aliasPresentation = myFactory.psiSingleReference(text(aliasName), () -> alias);
                InlayPresentation presentation = join(List.of(aliasPresentation, text("="), hint(type, level + 1)), "");
                innerPresentations.add(presentation);
            }
        }

        if (innerPresentations.isEmpty()) {
            return traitPresentation;
        } else {
            InlayPresentation expanded = join(innerPresentations, ", ");
            InlayPresentation traitTypesPresentation = myFactory.collapsible(
                text("<"), text(PLACEHOLDER),
                () -> expanded,
                text(">"), checkSize(level, innerPresentations.size())
            );
            return join(List.of(traitPresentation, traitTypesPresentation), "");
        }
    }

    private boolean checkSize(int level, int elementsCount) {
        return level + elementsCount > FOLDING_THRESHOLD;
    }

    private boolean isDefaultTypeParameter(@Nonnull Ty argument, @Nonnull RsTypeParameter parameter) {
        if (parameter.getTypeReference() == null) return false;
        return argument.isEquivalentTo(org.rust.lang.core.types.RsTypesUtil.getNormType(parameter.getTypeReference()));
    }

    private boolean isDefaultConstParameter(@Nonnull Const argument, @Nonnull RsConstParameter parameter) {
        if (parameter.getTypeReference() == null) return false;
        Ty expectedTy = org.rust.lang.core.types.RsTypesUtil.getNormType(parameter.getTypeReference());
        if (parameter.getExpr() == null) return false;
        Const defaultValue = org.rust.lang.utils.evaluation.ConstExprEvaluator.evaluate(parameter.getExpr(), expectedTy);
        return !(defaultValue instanceof CtUnknown) && argument.equals(defaultValue);
    }

    private boolean isDefaultTypeAlias(@Nonnull Ty argument, @Nonnull RsTypeAlias alias) {
        if (alias.getTypeReference() == null) return false;
        return argument.isEquivalentTo(org.rust.lang.core.types.RsTypesUtil.getNormType(alias.getTypeReference()));
    }

    @Nonnull
    private InlayPresentation join(@Nonnull List<InlayPresentation> presentations, @Nonnull String separator) {
        if (separator.isEmpty()) {
            return myFactory.seq(presentations.toArray(new InlayPresentation[0]));
        }
        List<InlayPresentation> result = new ArrayList<>();
        boolean first = true;
        for (InlayPresentation presentation : presentations) {
            if (!first) {
                result.add(text(separator));
            }
            result.add(presentation);
            first = false;
        }
        return myFactory.seq(result.toArray(new InlayPresentation[0]));
    }

    @Nonnull
    private InlayPresentation text(@Nullable String text) {
        return myFactory.smallText(text != null ? text : "?");
    }

    @Nonnull
    private static <A, B> List<Pair<A, B>> zipTypeArgs(@Nonnull List<Ty> types, @Nonnull List<RsTypeParameter> params) {
        List<Pair<A, B>> result = new ArrayList<>();
        int size = Math.min(types.size(), params.size());
        for (int i = 0; i < size; i++) {
            @SuppressWarnings("unchecked")
            Pair<A, B> pair = (Pair<A, B>) new Pair<>(types.get(i), params.get(i));
            result.add(pair);
        }
        return result;
    }

    @Nonnull
    private static <A, B> List<Pair<A, B>> zipConstArgs(@Nonnull List<Const> consts, @Nonnull List<RsConstParameter> params) {
        List<Pair<A, B>> result = new ArrayList<>();
        int size = Math.min(consts.size(), params.size());
        for (int i = 0; i < size; i++) {
            @SuppressWarnings("unchecked")
            Pair<A, B> pair = (Pair<A, B>) new Pair<>(consts.get(i), params.get(i));
            result.add(pair);
        }
        return result;
    }

    private static class Pair<A, B> {
        private final A myFirst;
        private final B mySecond;

        Pair(@Nonnull A first, @Nonnull B second) {
            myFirst = first;
            mySecond = second;
        }
    }
}
