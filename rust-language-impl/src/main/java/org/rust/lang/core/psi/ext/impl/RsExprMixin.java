/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.lang.core.psi.ext.impl;

import consulo.language.ast.ASTNode;
import consulo.language.psi.PsiElement;
import consulo.language.psi.stub.IStubElementType;
import jakarta.annotation.Nonnull;
import org.rust.lang.core.macros.RsExpandedElement;
import org.rust.lang.core.psi.RsExpr;
import org.rust.lang.core.stubs.RsPlaceholderStub;
import org.rust.lang.core.psi.ext.*;

public abstract class RsExprMixin extends RsStubbedElementImpl<RsPlaceholderStub<?>> implements RsExpr {
    public RsExprMixin(@Nonnull ASTNode node) {
        super(node);
    }

    public RsExprMixin(@Nonnull RsPlaceholderStub<?> stub, @Nonnull IStubElementType nodeType) {
        super(stub, nodeType);
    }

    @Override
    public PsiElement getContext() {
        return RsExpandedElement.getContextImpl(this);
    }
}
