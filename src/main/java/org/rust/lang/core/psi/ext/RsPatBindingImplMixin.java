/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.lang.core.psi.ext;

import com.intellij.navigation.ItemPresentation;
import com.intellij.psi.PsiElement;
import com.intellij.psi.search.SearchScope;
import com.intellij.psi.tree.IElementType;
import com.intellij.psi.util.PsiTreeUtil;
import org.jetbrains.annotations.NotNull;
import org.rust.ide.icons.RsIcons;
import org.rust.ide.presentation.PresentationUtil;
import org.rust.lang.core.psi.ext.RsPatBindingUtil;
import org.rust.lang.core.psi.*;
import org.rust.lang.core.resolve.ref.RsPatBindingReferenceImpl;
import org.rust.lang.core.resolve.ref.RsReference;

import javax.swing.*;

public abstract class RsPatBindingImplMixin extends RsNamedElementImpl implements RsPatBinding {

    public RsPatBindingImplMixin(@NotNull IElementType type) {
        super(type);
    }

    @NotNull
    @Override
    public RsReference getReference() {
        return new RsPatBindingReferenceImpl(this);
    }

    @NotNull
    @Override
    public PsiElement getReferenceNameElement() {
        return getNameIdentifier();
    }

    @NotNull
    @Override
    public String getReferenceName() {
        return getName();
    }

    @Override
    public Icon getIcon(int flags) {
        boolean isArg = RsPatBindingUtil.isArg(this);
        boolean isMut = RsPatBindingUtil.getMutability(this).isMut();
        if (isArg && isMut) return RsIcons.MUT_ARGUMENT;
        if (isArg) return RsIcons.ARGUMENT;
        if (isMut) return RsIcons.MUT_BINDING;
        return RsIcons.BINDING;
    }

    @NotNull
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

    @NotNull
    @Override
    public ItemPresentation getPresentation() {
        return PresentationUtil.getPresentation(this);
    }
}
