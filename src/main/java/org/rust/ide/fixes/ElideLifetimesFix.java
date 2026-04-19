/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.ide.fixes;

import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.project.Project;
import com.intellij.psi.PsiElement;
import com.intellij.psi.util.PsiTreeUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
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

public class ElideLifetimesFix extends RsQuickFixBase<RsFunction> {

    public ElideLifetimesFix(@NotNull RsFunction element) {
        super(element);
    }

    @NotNull
    @Override
    public String getText() {
        return RsBundle.message("intention.name.elide.lifetimes");
    }

    @NotNull
    @Override
    public String getFamilyName() {
        return getText();
    }

    @Override
    public void invoke(@NotNull Project project, @Nullable Editor editor, @NotNull RsFunction element) {
        new LifetimeRemover().visitFunction(element);
    }

    private static class LifetimeRemover extends RsVisitor {
        private final List<RsLifetimeParameter> boundsLifetimes = new ArrayList<>();

        @Override
        public void visitFunction(@NotNull RsFunction fn) {
            if (fn.getTypeParameterList() != null) visitTypeParameterList(fn.getTypeParameterList());
            if (fn.getValueParameterList() != null) visitValueParameterList(fn.getValueParameterList());
            if (fn.getRetType() != null) visitRetType(fn.getRetType());
        }

        @Override
        public void visitTypeParameterList(@NotNull RsTypeParameterList typeParameters) {
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
        public void visitTypeArgumentList(@NotNull RsTypeArgumentList typeArguments) {
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
        public void visitValueParameterList(@NotNull RsValueParameterList valueParameters) {
            if (valueParameters.getSelfParameter() != null) visitSelfParameter(valueParameters.getSelfParameter());
            for (RsValueParameter param : valueParameters.getValueParameterList()) {
                visitValueParameter(param);
            }
        }

        @Override
        public void visitSelfParameter(@NotNull RsSelfParameter selfParameter) {
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
        public void visitRefLikeType(@NotNull RsRefLikeType refLike) {
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
        public void visitElement(@NotNull RsElement element) {
            Collection<RsElement> children = PsiTreeUtil.getChildrenOfTypeAsList(element, RsElement.class);
            for (RsElement child : children) {
                child.accept(this);
            }
        }
    }
}
