/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.lang.core.psi.ext;

import consulo.language.ast.ASTNode;
import consulo.language.psi.PsiElement;
import consulo.content.scope.SearchScope;
import consulo.language.psi.stub.IStubElementType;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import org.rust.ide.icons.RsIcons;
import org.rust.lang.core.macros.RsExpandedElement;
import org.rust.lang.core.psi.RsModDeclItem;
import org.rust.lang.core.psi.RsPsiImplUtil;
import org.rust.lang.core.resolve.ref.RsModReferenceImpl;
import org.rust.lang.core.resolve.ref.RsReference;
import org.rust.lang.core.stubs.RsModDeclItemStub;

import javax.swing.*;
import org.rust.lang.core.psi.ext.RsVisibilityUtil;
import consulo.ui.image.Image;

public abstract class RsModDeclItemImplMixin extends RsStubbedNamedElementImpl<RsModDeclItemStub> implements RsModDeclItem {

    public RsModDeclItemImplMixin(@Nonnull ASTNode node) {
        super(node);
    }

    public RsModDeclItemImplMixin(@Nonnull RsModDeclItemStub stub, @Nonnull IStubElementType nodeType) {
        super(stub, nodeType);
    }

    @Nonnull
    @Override
    public RsReference getReference() {
        return new RsModReferenceImpl(this);
    }

    @Nonnull
    @Override
    public PsiElement getReferenceNameElement() {
        return getIdentifier();
    }

    @Nonnull
    @Override
    public String getReferenceName() {
        return getName();
    }

    public consulo.ui.image.Image getIcon(int flags) {
        return RsVisibilityUtil.iconWithVisibility(this, flags, RsIcons.MODULE);
    }

    @Nullable
    @Override
    public String getCrateRelativePath() {
        return RsPsiImplUtil.crateRelativePath(this);
    }

    @Override
    public PsiElement getContext() {
        return RsExpandedElement.getContextImpl(this);
    }

    @Nonnull
    @Override
    public SearchScope getUseScope() {
        SearchScope scope = RsPsiImplUtil.getDeclarationUseScope(this);
        return scope != null ? scope : super.getUseScope();
    }
}
