/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.lang.core.psi.ext.impl;

import consulo.language.ast.ASTNode;
import consulo.language.psi.PsiElement;
import consulo.content.scope.SearchScope;
import consulo.language.psi.stub.IStubElementType;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import org.rust.lang.core.psi.RsLifetimeParameter;
import org.rust.lang.core.psi.impl.RsPsiFactory;
import org.rust.lang.core.psi.impl.RsPsiImplUtil;
import org.rust.lang.core.stubs.RsLifetimeParameterStub;
import org.rust.lang.core.psi.ext.*;

public abstract class RsLifetimeParameterImplMixin extends RsStubbedNamedElementImpl<RsLifetimeParameterStub>
    implements RsLifetimeParameter {

    public RsLifetimeParameterImplMixin(@Nonnull ASTNode node) {
        super(node);
    }

    public RsLifetimeParameterImplMixin(@Nonnull RsLifetimeParameterStub stub,
                                        @Nonnull IStubElementType nodeType) {
        super(stub, nodeType);
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
