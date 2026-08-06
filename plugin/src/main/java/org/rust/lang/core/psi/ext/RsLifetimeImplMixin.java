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
import org.rust.lang.core.psi.RsLifetime;
import org.rust.lang.core.psi.RsPsiFactory;
import org.rust.lang.core.psi.RsPsiImplUtil;
import org.rust.lang.core.resolve.ref.RsLifetimeReferenceImpl;
import org.rust.lang.core.resolve.ref.RsReference;
import org.rust.lang.core.stubs.RsLifetimeStub;

public abstract class RsLifetimeImplMixin extends RsStubbedNamedElementImpl<RsLifetimeStub>
    implements RsLifetime {

    public RsLifetimeImplMixin(@Nonnull ASTNode node) {
        super(node);
    }

    public RsLifetimeImplMixin(@Nonnull RsLifetimeStub stub, @Nonnull IStubElementType nodeType) {
        super(stub, nodeType);
    }

    @Nonnull
    @Override
    public PsiElement getReferenceNameElement() {
        return getNameIdentifier();
    }

    @Nonnull
    @Override
    public String getReferenceName() {
        RsLifetimeStub stub = getGreenStub();
        if (stub != null) return stub.getName();
        return getReferenceNameElement().getText();
    }

    @Nonnull
    @Override
    public RsReference getReference() {
        return new RsLifetimeReferenceImpl(this);
    }

    @Nonnull
    @Override
    public PsiElement getNameIdentifier() {
        return getQuoteIdentifier();
    }

    @Nullable
    @Override
    public PsiElement setName(@Nonnull String name) {
        getNameIdentifier().replace(new RsPsiFactory(getProject()).createQuoteIdentifier(name));
        return this;
    }

    @Nonnull
    @Override
    public SearchScope getUseScope() {
        SearchScope scope = RsPsiImplUtil.getParameterUseScope(this);
        return scope != null ? scope : super.getUseScope();
    }
}
