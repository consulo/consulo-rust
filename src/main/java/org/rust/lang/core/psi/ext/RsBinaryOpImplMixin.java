/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.lang.core.psi.ext;

import com.intellij.lang.ASTNode;
import com.intellij.psi.PsiElement;
import com.intellij.psi.stubs.IStubElementType;
import org.jetbrains.annotations.NotNull;
import org.rust.lang.core.psi.RsBinaryOp;
import org.rust.lang.core.resolve.ref.RsBinaryOpReferenceImpl;
import org.rust.lang.core.resolve.ref.RsReference;
import org.rust.lang.core.stubs.RsBinaryOpStub;

public abstract class RsBinaryOpImplMixin extends RsStubbedElementImpl<RsBinaryOpStub> implements RsBinaryOp {

    public RsBinaryOpImplMixin(@NotNull ASTNode node) {
        super(node);
    }

    public RsBinaryOpImplMixin(@NotNull RsBinaryOpStub stub, @NotNull IStubElementType<?, ?> nodeType) {
        super(stub, nodeType);
    }

    @NotNull
    @Override
    public PsiElement getReferenceNameElement() {
        return RsBinaryOpImplUtil.getOperator(this);
    }

    @NotNull
    @Override
    public RsReference getReference() {
        return new RsBinaryOpReferenceImpl(this);
    }
}
