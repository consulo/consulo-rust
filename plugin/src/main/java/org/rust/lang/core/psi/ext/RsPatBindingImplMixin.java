/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.lang.core.psi.ext;

import consulo.navigation.ItemPresentation;
import consulo.language.psi.PsiElement;
import consulo.content.scope.SearchScope;
import consulo.language.ast.ASTNode;
import consulo.language.psi.util.PsiTreeUtil;
import jakarta.annotation.Nonnull;
import org.rust.ide.icons.RsIcons;
import org.rust.ide.presentation.PresentationUtil;
import org.rust.lang.core.psi.ext.RsPatBindingUtil;
import org.rust.lang.core.psi.*;
import org.rust.lang.core.resolve.ref.RsPatBindingReferenceImpl;
import org.rust.lang.core.resolve.ref.RsReference;

import javax.swing.*;
import consulo.ui.image.Image;

public abstract class RsPatBindingImplMixin extends RsNamedElementImpl implements RsPatBinding {

    public RsPatBindingImplMixin(@Nonnull ASTNode type) {
        super(type);
    }

    @Nonnull
    @Override
    public RsReference getReference() {
        return new RsPatBindingReferenceImpl(this);
    }

    @Nonnull
    @Override
    public PsiElement getReferenceNameElement() {
        return getNameIdentifier();
    }

    @Nonnull
    @Override
    public String getReferenceName() {
        return getName();
    }

    public consulo.ui.image.Image getIcon(int flags) {
        boolean isArg = RsPatBindingUtil.isArg(this);
        boolean isMut = RsPatBindingUtil.getMutability(this).isMut();
        if (isArg && isMut) return RsIcons.MUT_ARGUMENT;
        if (isArg) return RsIcons.ARGUMENT;
        if (isMut) return RsIcons.MUT_BINDING;
        return RsIcons.BINDING;
    }

    @Nonnull
    @Override
    public SearchScope getUseScope() {
        PsiElement owner = PsiTreeUtil.getContextOfType(this,
            RsBlock.class,
            RsFunction.class,
            RsLambdaExpr.class
        );
        if (owner != null) return RsPsiImplUtil.localOrMacroSearchScope(owner);
        return super.getUseScope();
    }

    @Nonnull
    @Override
    public ItemPresentation getPresentation() {
        return PresentationUtil.getPresentation(this);
    }
}
