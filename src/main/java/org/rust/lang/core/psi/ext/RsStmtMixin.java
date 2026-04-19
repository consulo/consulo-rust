/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.lang.core.psi.ext;

import com.intellij.lang.ASTNode;
import com.intellij.psi.PsiElement;
import com.intellij.psi.stubs.IStubElementType;
import com.intellij.psi.stubs.StubBase;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.rust.lang.core.macros.RsExpandedElement;
import org.rust.lang.core.psi.RsStmt;

public abstract class RsStmtMixin extends RsStubbedElementImpl<StubBase<?>> implements RsStmt {

    public RsStmtMixin(@NotNull ASTNode node) {
        super(node);
    }

    public RsStmtMixin(@NotNull StubBase<?> stub, @NotNull IStubElementType<?, ?> nodeType) {
        super(stub, nodeType);
    }

    @Nullable
    @Override
    public PsiElement getContext() {
        return RsExpandedElement.getContextImpl(this);
    }
}
