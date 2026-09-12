/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.ide.fixes;

import consulo.codeEditor.Editor;
import consulo.project.Project;
import consulo.language.psi.PsiElement;
import consulo.language.psi.util.PsiTreeUtil;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import org.rust.RsBundle;
import org.rust.lang.core.psi.*;
import org.rust.lang.core.psi.ext.*;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;
import org.rust.lang.core.psi.ext.RsRefLikeTypeUtil;
import org.rust.lang.core.psi.ext.RsSelfParameterUtil;
import org.rust.lang.core.psi.ext.RsElement;
import consulo.localize.LocalizeValue;

public class ElideLifetimesFix extends RsQuickFixBase<RsFunction> {

    public ElideLifetimesFix(@Nonnull RsFunction element) {
        super(element);
    }

    @Nonnull
    @Override
    public consulo.localize.LocalizeValue getText() {
        return consulo.localize.LocalizeValue.of(RsBundle.message("intention.name.elide.lifetimes"));
    }

    @Nonnull
        public consulo.localize.LocalizeValue getFamilyName() {
        return getText();
    }

    @Override
    public void invoke(@Nonnull Project project, @Nullable Editor editor, @Nonnull RsFunction element) {
        new LifetimeRemover().visitFunction(element);
    }

    private static class LifetimeRemover extends RsVisitor {
        private final List<RsLifetimeParameter> boundsLifetimes = new ArrayList<>();

        @Override
        public void visitFunction(@Nonnull RsFunction fn) {
            if (fn.getTypeParameterList() != null) visitTypeParameterList(fn.getTypeParameterList());
            if (fn.getValueParameterList() != null) visitValueParameterList(fn.getValueParameterList());
            if (fn.getRetType() != null) visitRetType(fn.getRetType());
        }

        @Override
        public void visitTypeParameterList(@Nonnull RsTypeParameterList typeParameters) {
            boundsLifetimes.addAll(typeParameters.getLifetimeParameterList());
            List<String> typeNames = RsTypeParameterListUtil.getGenericParameters(typeParameters, false)
                .stream().map(PsiElement::getText).collect(Collectors.toList());
            if (typeNames.isEmpty()) {
                typeParameters.delete();
            } else {
                RsTypeParameterList types = new RsPsiFactory(typeParameters.getProject()).createTypeParameterList(typeNames);
                typeParameters.replace(types);
            }
        }

        @Override
        public void visitTypeArgumentList(@Nonnull RsTypeArgumentList typeArguments) {
            super.visitTypeArgumentList(typeArguments);
            List<String> restNames = RsTypeArgumentListUtil.getGenericArguments(typeArguments, false, true, true, true)
                .stream().map(PsiElement::getText).collect(Collectors.toList());
            if (restNames.isEmpty()) {
                typeArguments.delete();
            } else {
                RsTypeArgumentList newTypeArguments = new RsPsiFactory(typeArguments.getProject()).createTypeArgumentList(restNames);
                typeArguments.replace(newTypeArguments);
            }
        }

        @Override
        public void visitValueParameterList(@Nonnull RsValueParameterList valueParameters) {
            if (valueParameters.getSelfParameter() != null) visitSelfParameter(valueParameters.getSelfParameter());
            for (RsValueParameter param : valueParameters.getValueParameterList()) {
                visitValueParameter(param);
            }
        }

        @Override
        public void visitSelfParameter(@Nonnull RsSelfParameter selfParameter) {
            if (selfParameter.getLifetime() != null) {
                RsSelfParameter newSelfParameter = new RsPsiFactory(selfParameter.getProject())
                    .createSelfReference(RsSelfParameterUtil.getMutability(selfParameter).isMut());
                selfParameter.replace(newSelfParameter);
            }
            if (selfParameter.getTypeReference() != null) {
                selfParameter.getTypeReference().accept(this);
            }
        }

        @Override
        public void visitRefLikeType(@Nonnull RsRefLikeType refLike) {
            if (refLike.getTypeReference() != null) {
                visitTypeReference(refLike.getTypeReference());
            }
            if (RsRefLikeTypeUtil.isRef(refLike) && refLike.getLifetime() != null) {
                String typeRefText = refLike.getTypeReference() != null ? refLike.getTypeReference().getText() : "";
                RsRefLikeType ref = new RsPsiFactory(refLike.getProject())
                    .createReferenceType(typeRefText, RsRefLikeTypeUtil.getMutability(refLike));
                refLike.replace(ref);
            }
        }

        @Override
        public void visitElement(@Nonnull RsElement element) {
            Collection<RsElement> children = PsiTreeUtil.getChildrenOfTypeAsList(element, RsElement.class);
            for (RsElement child : children) {
                child.accept(this);
            }
        }
    }
}
