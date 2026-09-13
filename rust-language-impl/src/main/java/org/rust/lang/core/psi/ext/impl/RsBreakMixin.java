/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.lang.core.psi.ext.impl;

import consulo.language.ast.ASTNode;
import consulo.language.psi.PsiElement;
import consulo.language.psi.stub.IStubElementType;
import jakarta.annotation.Nonnull;
import org.rust.lang.core.psi.RsBreakExpr;
import org.rust.lang.core.stubs.RsPlaceholderStub;
import org.rust.lang.core.psi.ext.*;

public abstract class RsBreakMixin extends RsExprMixin implements RsBreakExpr {

    public RsBreakMixin(@Nonnull ASTNode node) {
        super(node);
    }

    public RsBreakMixin(@Nonnull RsPlaceholderStub<?> stub, @Nonnull IStubElementType nodeType) {
        super(stub, nodeType);
    }

    @Nonnull
    @Override
    public PsiElement getOperator() {
        return getBreak();
    }
}
