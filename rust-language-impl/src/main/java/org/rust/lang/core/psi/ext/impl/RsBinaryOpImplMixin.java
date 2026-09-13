/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.lang.core.psi.ext.impl;

import consulo.language.ast.ASTNode;
import consulo.language.psi.PsiElement;
import consulo.language.psi.stub.IStubElementType;
import jakarta.annotation.Nonnull;
import org.rust.lang.core.psi.RsBinaryOp;
import org.rust.lang.core.resolve.ref.RsBinaryOpReferenceImpl;
import org.rust.lang.core.resolve.ref.RsReference;
import org.rust.lang.core.stubs.RsBinaryOpStub;
import org.rust.lang.core.psi.ext.*;

public abstract class RsBinaryOpImplMixin extends RsStubbedElementImpl<RsBinaryOpStub> implements RsBinaryOp {

    public RsBinaryOpImplMixin(@Nonnull ASTNode node) {
        super(node);
    }

    public RsBinaryOpImplMixin(@Nonnull RsBinaryOpStub stub, @Nonnull IStubElementType nodeType) {
        super(stub, nodeType);
    }

    @Nonnull
    @Override
    public PsiElement getReferenceNameElement() {
        return RsBinaryOpImplUtil.getOperator(this);
    }

    @Nonnull
    @Override
    public RsReference getReference() {
        return new RsBinaryOpReferenceImpl(this);
    }
}
