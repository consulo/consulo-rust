/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.lang.core.psi.ext;

import com.intellij.lang.ASTNode;
import com.intellij.psi.PsiElement;
import com.intellij.psi.stubs.IStubElementType;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.rust.lang.core.psi.RsIndexExpr;
import org.rust.lang.core.psi.impl.RsExprImpl;
import org.rust.lang.core.resolve.ref.RsIndexExprReferenceImpl;
import org.rust.lang.core.resolve.ref.RsReference;
import org.rust.lang.core.stubs.RsPlaceholderStub;

public abstract class RsIndexExprImplMixin extends RsExprImpl implements RsIndexExpr {

    public RsIndexExprImplMixin(@NotNull ASTNode node) {
        super(node);
    }

    public RsIndexExprImplMixin(@NotNull RsPlaceholderStub<?> stub, @NotNull IStubElementType<?, ?> nodeType) {
        super(stub, nodeType);
    }

    @Nullable
    @Override
    public PsiElement getReferenceNameElement() {
        return null;
    }

    @NotNull
    @Override
    public RsReference getReference() {
        return new RsIndexExprReferenceImpl(this);
    }
}
