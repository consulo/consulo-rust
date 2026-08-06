/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.lang.core.psi.ext;

import consulo.language.ast.ASTNode;
import consulo.language.psi.PsiElement;
import consulo.language.psi.stub.IStubElementType;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import org.rust.lang.core.psi.RsIndexExpr;
import org.rust.lang.core.psi.impl.RsExprImpl;
import org.rust.lang.core.resolve.ref.RsIndexExprReferenceImpl;
import org.rust.lang.core.resolve.ref.RsReference;
import org.rust.lang.core.stubs.RsPlaceholderStub;

public abstract class RsIndexExprImplMixin extends RsExprImpl implements RsIndexExpr {

    public RsIndexExprImplMixin(@Nonnull ASTNode node) {
        super(node);
    }

    public RsIndexExprImplMixin(@Nonnull RsPlaceholderStub<?> stub, @Nonnull IStubElementType nodeType) {
        super(stub, nodeType);
    }

    @Nullable
    @Override
    public PsiElement getReferenceNameElement() {
        return null;
    }

    @Nonnull
    @Override
    public RsReference getReference() {
        return new RsIndexExprReferenceImpl(this);
    }
}
