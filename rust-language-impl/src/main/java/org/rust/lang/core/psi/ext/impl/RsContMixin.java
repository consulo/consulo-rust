/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.lang.core.psi.ext.impl;

import consulo.language.ast.ASTNode;
import consulo.language.psi.PsiElement;
import consulo.language.psi.stub.IStubElementType;
import jakarta.annotation.Nonnull;
import org.rust.lang.core.psi.RsContExpr;
import org.rust.lang.core.stubs.RsPlaceholderStub;
import org.rust.lang.core.psi.ext.*;

public abstract class RsContMixin extends RsExprMixin implements RsContExpr {

    public RsContMixin(@Nonnull ASTNode node) {
        super(node);
    }

    public RsContMixin(@Nonnull RsPlaceholderStub<?> stub, @Nonnull IStubElementType nodeType) {
        super(stub, nodeType);
    }

    @Nonnull
    @Override
    public PsiElement getOperator() {
        return getContinue();
    }
}
