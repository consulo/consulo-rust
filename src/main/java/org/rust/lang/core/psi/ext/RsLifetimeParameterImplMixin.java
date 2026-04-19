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
import org.rust.lang.core.psi.RsLifetimeParameter;
import org.rust.lang.core.psi.RsPsiFactory;
import org.rust.lang.core.psi.RsPsiImplUtil;
import org.rust.lang.core.stubs.RsLifetimeParameterStub;

public abstract class RsLifetimeParameterImplMixin extends RsStubbedNamedElementImpl<RsLifetimeParameterStub>
    implements RsLifetimeParameter {

    public RsLifetimeParameterImplMixin(@NotNull ASTNode node) {
        super(node);
    }

    public RsLifetimeParameterImplMixin(@NotNull RsLifetimeParameterStub stub,
                                        @NotNull IStubElementType<?, ?> nodeType) {
        super(stub, nodeType);
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
