/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.lang.core.psi.ext;

import com.intellij.lang.ASTNode;
import com.intellij.psi.PsiElement;
import com.intellij.psi.stubs.IStubElementType;
import org.jetbrains.annotations.NotNull;
import org.rust.lang.core.macros.RsExpandedElement;
import org.rust.lang.core.psi.RsExpr;
import org.rust.lang.core.stubs.RsPlaceholderStub;

public abstract class RsExprMixin extends RsStubbedElementImpl<RsPlaceholderStub<?>> implements RsExpr {
    public RsExprMixin(@NotNull ASTNode node) {
        super(node);
    }

    public RsExprMixin(@NotNull RsPlaceholderStub<?> stub, @NotNull IStubElementType<?, ?> nodeType) {
        super(stub, nodeType);
    }

    @Override
    public PsiElement getContext() {
        return RsExpandedElement.getContextImpl(this);
    }
}
