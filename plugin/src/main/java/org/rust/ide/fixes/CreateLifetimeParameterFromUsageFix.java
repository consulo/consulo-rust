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
import java.util.List;
import java.util.stream.Collectors;
import org.rust.lang.core.psi.ext.RsElement;

public class CreateLifetimeParameterFromUsageFix extends RsQuickFixBase<RsLifetime> {

    public CreateLifetimeParameterFromUsageFix(@Nonnull RsLifetime lifetime) {
        super(lifetime);
    }

    @Nonnull
        public consulo.localize.LocalizeValue getFamilyName() {
        return consulo.localize.LocalizeValue.of(RsBundle.message("intention.family.name.create.lifetime.parameter"));
    }

    @Nonnull
    @Override
    public consulo.localize.LocalizeValue getText() {
        return getFamilyName();
    }

    @Override
    public void invoke(@Nonnull Project project, @Nullable Editor editor, @Nonnull RsLifetime element) {
        Context context = gatherContext(element);
        if (context == null) return;
        insertLifetime(context.declaration, project, context.lifetime);
    }

    private void insertLifetime(@Nonnull RsGenericDeclaration declaration, @Nonnull Project project, @Nonnull RsLifetime startElement) {
        RsTypeParameterList originalParams = declaration.getTypeParameterList();
        RsPsiFactory factory = new RsPsiFactory(project);
        if (originalParams != null) {
            List<RsElement> parameters = new ArrayList<>();
            parameters.addAll(originalParams.getLifetimeParameterList());
            parameters.add(startElement);
            parameters.addAll(RsTypeParameterListUtil.getGenericParameters(originalParams, false));
            String paramText = parameters.stream().map(PsiElement::getText).collect(Collectors.joining(", "));
            RsTypeParameterList parameterList = factory.createTypeParameterList(paramText);
            originalParams.replace(parameterList);
        } else {
            RsTypeParameterList parameterList = factory.createTypeParameterList(startElement.getText());
            if (!(declaration instanceof RsNameIdentifierOwner)) return;
            PsiElement nameIdentifier = ((RsNameIdentifierOwner) declaration).getNameIdentifier();
            if (nameIdentifier != null) {
                declaration.addAfter(parameterList, nameIdentifier);
            }
        }
    }

    @Nullable
    public static CreateLifetimeParameterFromUsageFix tryCreate(@Nonnull RsLifetime lifetime) {
        return gatherContext(lifetime) != null ? new CreateLifetimeParameterFromUsageFix(lifetime) : null;
    }

    @Nullable
    private static Context gatherContext(@Nonnull RsLifetime element) {
        RsGenericDeclaration genericDeclaration = PsiTreeUtil.getParentOfType(element, RsGenericDeclaration.class, false);
        if (!(genericDeclaration instanceof RsNameIdentifierOwner)) return null;
        return new Context(element, genericDeclaration);
    }

    private static class Context {
        final RsLifetime lifetime;
        final RsGenericDeclaration declaration;

        Context(@Nonnull RsLifetime lifetime, @Nonnull RsGenericDeclaration declaration) {
            this.lifetime = lifetime;
            this.declaration = declaration;
        }
    }
}
