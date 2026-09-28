/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.ide.hints.type;

import consulo.language.editor.inlay.CollapseState;
import consulo.language.editor.inlay.DeclarativeCollapsiblePresentationTreeBuilder;
import consulo.language.editor.inlay.DeclarativePresentationTreeBuilder;
import consulo.language.editor.inlay.InlayActionData;
import consulo.language.editor.inlay.InlayActionPayload;
import consulo.language.psi.PsiElement;
import consulo.language.psi.SmartPointerManager;
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
import org.rust.lang.core.types.RsTypesUtil;
import org.rust.lang.core.types.consts.Const;
import org.rust.lang.core.types.consts.CtConstParameter;
import org.rust.lang.core.types.consts.CtUnknown;
import org.rust.lang.core.types.consts.CtValue;
import org.rust.lang.core.types.ty.*;
import org.rust.lang.utils.evaluation.ConstExprEvaluator;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.function.Consumer;

public class RsTypeHintsFactory {

    private static final String PLACEHOLDER = "…";
    private static final int FOLDING_THRESHOLD = 3;

    private final boolean myShowObviousTypes;

    public RsTypeHintsFactory(boolean showObviousTypes) {
        myShowObviousTypes = showObviousTypes;
    }

    public void typeHint(@Nonnull Ty type, @Nonnull DeclarativePresentationTreeBuilder builder) {
        builder.text(": ");
        hint(type, 1, builder);
    }

    private void hint(@Nonnull Kind kind, int level, @Nonnull DeclarativePresentationTreeBuilder b) {
        if (kind instanceof Ty) {
            BoundElement<RsTypeAlias> alias = ((Ty) kind).getAliasedBy();
            if (alias != null) {
                aliasTypeHint(alias, level, b);
                return;
            }
        }

        if (kind instanceof TyTuple) tupleTypeHint((TyTuple) kind, level, b);
        else if (kind instanceof TyAdt) adtTypeHint((TyAdt) kind, level, b);
        else if (kind instanceof TyFunctionBase) functionTypeHint((TyFunctionBase) kind, level, b);
        else if (kind instanceof TyReference) referenceTypeHint((TyReference) kind, level, b);
        else if (kind instanceof TyPointer) pointerTypeHint((TyPointer) kind, level, b);
        else if (kind instanceof TyProjection) projectionTypeHint((TyProjection) kind, level, b);
        else if (kind instanceof TyTypeParameter) typeParameterTypeHint((TyTypeParameter) kind, b);
        else if (kind instanceof TyArray) arrayTypeHint((TyArray) kind, level, b);
        else if (kind instanceof TySlice) sliceTypeHint((TySlice) kind, level, b);
        else if (kind instanceof TyTraitObject) traitObjectTypeHint((TyTraitObject) kind, level, b);
        else if (kind instanceof TyAnon) anonTypeHint((TyAnon) kind, level, b);
        else if (kind instanceof CtConstParameter) constParameterTypeHint((CtConstParameter) kind, b);
        else if (kind instanceof CtValue) text(kind.toString(), b);
        else if (kind instanceof Ty) text(TypeRendering.getShortPresentableText((Ty) kind), b);
        else text("?", b);
    }

    private void functionTypeHint(@Nonnull TyFunctionBase type, int level, @Nonnull DeclarativePresentationTreeBuilder b) {
        List<Ty> parameters = type.getParamTypes();
        Ty returnType = type.getRetType();
        boolean startWithPlaceholder = checkSize(level, parameters.size() + 1);

        if (parameters.isEmpty()) {
            text("fn()", b);
        }
        else {
            collapsible(b, "fn(", ")", startWithPlaceholder,
                inner -> parametersHint(new ArrayList<>(parameters), level + 1, inner));
        }

        if (!(returnType instanceof TyUnit)) {
            collapsible(b, " → ", "", startWithPlaceholder, inner -> hint(returnType, level + 1, inner));
        }
    }

    private void tupleTypeHint(@Nonnull TyTuple type, int level, @Nonnull DeclarativePresentationTreeBuilder b) {
        collapsible(b, "(", ")", checkSize(level, type.getTypes().size()),
            inner -> tupleTypesHint(type.getTypes(), level + 1, inner));
    }

    private void tupleTypesHint(@Nonnull List<Ty> types, int level, @Nonnull DeclarativePresentationTreeBuilder b) {
        if (types.size() == 1) {
            hint(types.get(0), level, b);
            text(",", b);
            return;
        }
        join(types, ty -> hint(ty, level, b), () -> text(", ", b));
    }

    private void adtTypeHint(@Nonnull TyAdt type, int level, @Nonnull DeclarativePresentationTreeBuilder b) {
        List<Argument<Ty, RsTypeParameter>> typeArguments =
            zip(type.getTypeArguments(), RsGenericDeclarationUtil.getTypeParameters(type.getItem()));
        List<Argument<Const, RsConstParameter>> constArguments =
            zip(type.getConstArguments(), RsGenericDeclarationUtil.getConstParameters(type.getItem()));
        withGenericsTypeHint(
            inner -> reference(type.getItem().getName(), type.getItem(), inner),
            typeArguments, constArguments, level, b);
    }

    private void aliasTypeHint(@Nonnull BoundElement<RsTypeAlias> boundElement, int level, @Nonnull DeclarativePresentationTreeBuilder b) {
        RsTypeAlias alias = boundElement.getTypedElement();

        List<Argument<Ty, RsTypeParameter>> typeArguments = new ArrayList<>();
        for (RsTypeParameter param : RsGenericDeclarationUtil.getTypeParameters(alias)) {
            Ty ty = boundElement.getSubst().get(param);
            typeArguments.add(new Argument<>(ty != null ? ty : TyUnknown.INSTANCE, param));
        }
        List<Argument<Const, RsConstParameter>> constArguments = new ArrayList<>();
        for (RsConstParameter param : RsGenericDeclarationUtil.getConstParameters(alias)) {
            Const c = boundElement.getSubst().get(param);
            constArguments.add(new Argument<>(c != null ? c : CtUnknown.INSTANCE, param));
        }

        withGenericsTypeHint(
            inner -> reference(alias.getName(), alias, inner),
            typeArguments, constArguments, level, b);
    }

    private void projectionTypeHint(@Nonnull TyProjection type, int level, @Nonnull DeclarativePresentationTreeBuilder b) {
        RsTypeAlias target = type.getTarget().getTypedElement();
        List<Argument<Ty, RsTypeParameter>> typeArguments =
            zip(BoundElement.positionalTypeArguments(type.getTarget()), RsGenericDeclarationUtil.getTypeParameters(target));

        withGenericsTypeHint(
            inner -> {
                collapsible(inner, "<", ">", checkSize(level, 2), nested -> {
                    hint(type.getType(), level + 1, nested);
                    text(" as ", nested);
                    traitItemTypeHint(type.getTrait(), level + 1, false, nested);
                });
                text("::", inner);
                reference(target.getName(), target, inner);
            },
            typeArguments, Collections.emptyList(), level, b);
    }

    private void withGenericsTypeHint(
        @Nonnull Consumer<DeclarativePresentationTreeBuilder> typeName,
        @Nonnull List<Argument<Ty, RsTypeParameter>> typeArguments,
        @Nonnull List<Argument<Const, RsConstParameter>> constArguments,
        int level,
        @Nonnull DeclarativePresentationTreeBuilder b
    ) {
        List<Argument<?, ?>> all = new ArrayList<>();
        all.addAll(typeArguments);
        all.addAll(constArguments);
        all.sort((x, y) -> Integer.compare(x.parameter.getTextOffset(), y.parameter.getTextOffset()));

        List<Kind> userVisibleKindArguments = new ArrayList<>();
        for (Argument<?, ?> entry : all) {
            if (!myShowObviousTypes && entry.isDefault()) continue;
            userVisibleKindArguments.add((Kind) entry.argument);
        }

        typeName.accept(b);

        if (!userVisibleKindArguments.isEmpty()) {
            collapsible(b, "<", ">", checkSize(level, userVisibleKindArguments.size()),
                inner -> parametersHint(userVisibleKindArguments, level + 1, inner));
        }
    }

    private void referenceTypeHint(@Nonnull TyReference type, int level, @Nonnull DeclarativePresentationTreeBuilder b) {
        text("&" + (type.getMutability().isMut() ? "mut " : ""), b);
        hint(type.getReferenced(), level, b);
    }

    private void pointerTypeHint(@Nonnull TyPointer type, int level, @Nonnull DeclarativePresentationTreeBuilder b) {
        text("*" + (type.getMutability().isMut() ? "mut " : "const "), b);
        hint(type.getReferenced(), level, b);
    }

    private void typeParameterTypeHint(@Nonnull TyTypeParameter type, @Nonnull DeclarativePresentationTreeBuilder b) {
        TyTypeParameter.TypeParameter parameter = type.getParameter();
        if (parameter instanceof TyTypeParameter.Named named) {
            reference(named.getParameter().getName(), named.getParameter(), b);
            return;
        }
        text(type.toString(), b);
    }

    private void constParameterTypeHint(@Nonnull CtConstParameter constParam, @Nonnull DeclarativePresentationTreeBuilder b) {
        reference(constParam.getParameter().getName(), constParam.getParameter(), b);
    }

    private void arrayTypeHint(@Nonnull TyArray type, int level, @Nonnull DeclarativePresentationTreeBuilder b) {
        collapsible(b, "[", "]", checkSize(level, 1), inner -> {
            hint(type.getBase(), level + 1, inner);
            text("; ", inner);
            text(type.getSize() != null ? type.getSize().toString() : "?", inner);
        });
    }

    private void sliceTypeHint(@Nonnull TySlice type, int level, @Nonnull DeclarativePresentationTreeBuilder b) {
        collapsible(b, "[", "]", checkSize(level, 1), inner -> hint(type.getElementType(), level + 1, inner));
    }

    private void traitObjectTypeHint(@Nonnull TyTraitObject type, int level, @Nonnull DeclarativePresentationTreeBuilder b) {
        collapsible(b, "dyn ", "", checkSize(level, 1),
            inner -> join(type.getTraits(),
                trait -> traitItemTypeHint(trait, level + 1, true, inner),
                () -> text("+", inner)));
    }

    private void anonTypeHint(@Nonnull TyAnon type, int level, @Nonnull DeclarativePresentationTreeBuilder b) {
        collapsible(b, "impl ", "", checkSize(level, type.getTraits().size()),
            inner -> join(type.getTraits(),
                trait -> traitItemTypeHint(trait, level + 1, true, inner),
                () -> text("+", inner)));
    }

    private void parametersHint(@Nonnull List<? extends Kind> kinds, int level, @Nonnull DeclarativePresentationTreeBuilder b) {
        join(kinds, kind -> hint(kind, level, b), () -> text(", ", b));
    }

    private void traitItemTypeHint(
        @Nonnull BoundElement<RsTraitItem> trait,
        int level,
        boolean includeAssoc,
        @Nonnull DeclarativePresentationTreeBuilder b
    ) {
        RsTraitItem traitItem = trait.getTypedElement();
        reference(traitItem.getName(), traitItem, b);

        List<Consumer<DeclarativePresentationTreeBuilder>> inner = new ArrayList<>();

        List<PsiElement> genericParams = new ArrayList<>();
        genericParams.addAll(RsGenericDeclarationUtil.getTypeParameters(traitItem));
        genericParams.addAll(RsGenericDeclarationUtil.getConstParameters(traitItem));
        genericParams.sort((x, y) -> Integer.compare(x.getTextOffset(), y.getTextOffset()));

        for (PsiElement parameter : genericParams) {
            if (parameter instanceof RsTypeParameter typeParameter) {
                Ty argument = trait.getSubst().get(typeParameter);
                if (argument == null) continue;
                if (!myShowObviousTypes && isDefaultTypeParameter(argument, typeParameter)) continue;
                inner.add(nested -> hint(argument, level + 1, nested));
            }
            else if (parameter instanceof RsConstParameter constParameter) {
                Const argument = trait.getSubst().get(constParameter);
                if (argument == null) continue;
                if (!myShowObviousTypes && isDefaultConstParameter(argument, constParameter)) continue;
                inner.add(nested -> hint(argument, level + 1, nested));
            }
        }

        if (includeAssoc) {
            for (RsTypeAlias alias : traitItem.getAssociatedTypesTransitively()) {
                String aliasName = alias.getName();
                if (aliasName == null) continue;
                Ty type = trait.getAssoc().get(alias);
                if (type == null) continue;
                if (!myShowObviousTypes && isDefaultTypeAlias(type, alias)) continue;
                inner.add(nested -> {
                    reference(aliasName, alias, nested);
                    text("=", nested);
                    hint(type, level + 1, nested);
                });
            }
        }

        if (!inner.isEmpty()) {
            collapsible(b, "<", ">", checkSize(level, inner.size()),
                nested -> join(inner, part -> part.accept(nested), () -> text(", ", nested)));
        }
    }

    private void collapsible(
        @Nonnull DeclarativePresentationTreeBuilder b,
        @Nonnull String prefix,
        @Nonnull String suffix,
        boolean startWithPlaceholder,
        @Nonnull Consumer<DeclarativePresentationTreeBuilder> content
    ) {
        b.collapsibleList(
            startWithPlaceholder ? CollapseState.Collapsed : CollapseState.Expanded,
            expanded -> {
                toggle(expanded, prefix);
                content.accept(expanded);
                toggle(expanded, suffix);
            },
            collapsed -> toggle(collapsed, prefix + PLACEHOLDER + suffix)
        );
    }

    private static void toggle(@Nonnull DeclarativeCollapsiblePresentationTreeBuilder b, @Nonnull String text) {
        if (text.isEmpty()) return;
        b.toggleButton(nested -> nested.text(text));
    }

    private static void reference(@Nullable String name, @Nullable PsiElement target, @Nonnull DeclarativePresentationTreeBuilder b) {
        String text = name != null ? name : "?";
        if (target == null) {
            b.text(text);
            return;
        }
        b.text(text, new InlayActionData(
            new InlayActionPayload.PsiPointerInlayActionPayload(
                SmartPointerManager.getInstance(target.getProject()).createSmartPsiElementPointer(target)),
            RsInlayActionHandler.HANDLER_ID));
    }

    private static void text(@Nullable String text, @Nonnull DeclarativePresentationTreeBuilder b) {
        b.text(text != null ? text : "?");
    }

    private static <T> void join(@Nonnull Iterable<T> elements, @Nonnull Consumer<T> each, @Nonnull Runnable separator) {
        boolean first = true;
        for (T element : elements) {
            if (!first) {
                separator.run();
            }
            first = false;
            each.accept(element);
        }
    }

    private static boolean checkSize(int level, int elementsCount) {
        return level + elementsCount > FOLDING_THRESHOLD;
    }

    private static boolean isDefaultTypeParameter(@Nonnull Ty argument, @Nonnull RsTypeParameter parameter) {
        if (parameter.getTypeReference() == null) return false;
        return argument.isEquivalentTo(RsTypesUtil.getNormType(parameter.getTypeReference()));
    }

    private static boolean isDefaultConstParameter(@Nonnull Const argument, @Nonnull RsConstParameter parameter) {
        if (parameter.getTypeReference() == null) return false;
        if (parameter.getExpr() == null) return false;
        Ty expectedTy = RsTypesUtil.getNormType(parameter.getTypeReference());
        Const defaultValue = ConstExprEvaluator.evaluate(parameter.getExpr(), expectedTy);
        return !(defaultValue instanceof CtUnknown) && argument.equals(defaultValue);
    }

    private static boolean isDefaultTypeAlias(@Nonnull Ty argument, @Nonnull RsTypeAlias alias) {
        if (alias.getTypeReference() == null) return false;
        return argument.isEquivalentTo(RsTypesUtil.getNormType(alias.getTypeReference()));
    }

    private static <A, P extends PsiElement> List<Argument<A, P>> zip(@Nonnull List<? extends A> arguments, @Nonnull List<P> parameters) {
        List<Argument<A, P>> result = new ArrayList<>();
        int size = Math.min(arguments.size(), parameters.size());
        for (int i = 0; i < size; i++) {
            result.add(new Argument<>(arguments.get(i), parameters.get(i)));
        }
        return result;
    }

    private static class Argument<A, P extends PsiElement> {
        private final A argument;
        private final P parameter;

        Argument(@Nonnull A argument, @Nonnull P parameter) {
            this.argument = argument;
            this.parameter = parameter;
        }

        boolean isDefault() {
            if (argument instanceof Ty ty && parameter instanceof RsTypeParameter typeParameter) {
                return isDefaultTypeParameter(ty, typeParameter);
            }
            if (argument instanceof Const c && parameter instanceof RsConstParameter constParameter) {
                return isDefaultConstParameter(c, constParameter);
            }
            return false;
        }
    }
}
