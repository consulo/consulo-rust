/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.lang.core.psi.ext;

import com.intellij.lang.ASTNode;
import com.intellij.psi.PsiElement;
import com.intellij.psi.stubs.IStubElementType;
import org.jetbrains.annotations.NotNull;
import org.rust.lang.core.psi.RsContExpr;
import org.rust.lang.core.stubs.RsPlaceholderStub;

public abstract class RsContMixin extends RsExprMixin implements RsContExpr {

    public RsContMixin(@NotNull ASTNode node) {
        super(node);
    }

    public RsContMixin(@NotNull RsPlaceholderStub<?> stub, @NotNull IStubElementType<?, ?> nodeType) {
        super(stub, nodeType);
    }

    @NotNull
    @Override
    public PsiElement getOperator() {
        return getContinue();
    }
}
