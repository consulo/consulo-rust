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
import org.rust.lang.core.psi.RsExternCrateItem;
import org.rust.lang.core.psi.RsPsiImplUtil;
import org.rust.lang.core.resolve.ref.RsExternCrateReferenceImpl;
import org.rust.lang.core.resolve.ref.RsReference;
import org.rust.lang.core.stubs.RsExternCrateItemStub;

import javax.swing.*;
import consulo.ui.image.Image;

public abstract class RsExternCrateItemImplMixin extends RsStubbedNamedElementImpl<RsExternCrateItemStub>
    implements RsExternCrateItem {

    public RsExternCrateItemImplMixin(@Nonnull ASTNode node) {
        super(node);
    }

    public RsExternCrateItemImplMixin(@Nonnull RsExternCrateItemStub stub,
                                      @Nonnull IStubElementType elementType) {
        super(stub, elementType);
    }

    @Nonnull
    @Override
    public RsReference getReference() {
        return new RsExternCrateReferenceImpl(this);
    }

    @Nonnull
    @Override
    public PsiElement getReferenceNameElement() {
        PsiElement id = getIdentifier();
        if (id == null) id = getSelf();
        if (id == null) {
            throw new IllegalStateException(
                "Extern crate must contain identifier: " + this + " " + getText()
                    + " at " + getContainingFile().getVirtualFile().getPath()
            );
        }
        return id;
    }

    @Nonnull
    @Override
    public String getReferenceName() {
        RsExternCrateItemStub stub = getGreenStub();
        if (stub != null) return stub.getName();
        return getReferenceNameElement().getText();
    }

    @Nullable
    @Override
    public String getName() {
        return getReferenceName();
    }

    @Nullable
    @Override
    public PsiElement getNameIdentifier() {
        return getReferenceNameElement();
    }

    public consulo.ui.image.Image getIcon(int flags) {
        return RsIcons.CRATE;
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
