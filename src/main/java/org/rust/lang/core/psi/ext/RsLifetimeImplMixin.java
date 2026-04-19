/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.lang.core.psi.ext;

import com.intellij.lang.ASTNode;
import com.intellij.psi.PsiElement;
import com.intellij.psi.search.SearchScope;
import com.intellij.psi.stubs.IStubElementType;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.rust.lang.core.psi.RsLifetime;
import org.rust.lang.core.psi.RsPsiFactory;
import org.rust.lang.core.psi.RsPsiImplUtil;
import org.rust.lang.core.resolve.ref.RsLifetimeReferenceImpl;
import org.rust.lang.core.resolve.ref.RsReference;
import org.rust.lang.core.stubs.RsLifetimeStub;

public abstract class RsLifetimeImplMixin extends RsStubbedNamedElementImpl<RsLifetimeStub>
    implements RsLifetime {

    public RsLifetimeImplMixin(@NotNull ASTNode node) {
        super(node);
    }

    public RsLifetimeImplMixin(@NotNull RsLifetimeStub stub, @NotNull IStubElementType<?, ?> nodeType) {
        super(stub, nodeType);
    }

    @NotNull
    @Override
    public PsiElement getReferenceNameElement() {
        return getNameIdentifier();
    }

    @NotNull
    @Override
    public String getReferenceName() {
        RsLifetimeStub stub = getGreenStub();
        if (stub != null) return stub.getName();
        return getReferenceNameElement().getText();
    }

    @NotNull
    @Override
    public RsReference getReference() {
        return new RsLifetimeReferenceImpl(this);
    }

    @NotNull
    @Override
    public PsiElement getNameIdentifier() {
        return getQuoteIdentifier();
    }

    @Nullable
    @Override
    public PsiElement setName(@NotNull String name) {
        getNameIdentifier().replace(new RsPsiFactory(getProject()).createQuoteIdentifier(name));
        return this;
    }

    @NotNull
    @Override
    public SearchScope getUseScope() {
        SearchScope scope = RsPsiImplUtil.getParameterUseScope(this);
        return scope != null ? scope : super.getUseScope();
    }
}
