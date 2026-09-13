/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.lang.core.psi.ext.impl;

import consulo.language.ast.ASTNode;
import consulo.language.psi.PsiElement;
import consulo.language.psi.stub.IStubElementType;
import consulo.language.psi.stub.StubBase;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import org.rust.lang.core.macros.RsExpandedElement;
import org.rust.lang.core.psi.RsStmt;
import org.rust.lang.core.psi.ext.*;

public abstract class RsStmtMixin extends RsStubbedElementImpl<StubBase> implements RsStmt {

    public RsStmtMixin(@Nonnull ASTNode node) {
        super(node);
    }

    public RsStmtMixin(@Nonnull StubBase stub, @Nonnull IStubElementType nodeType) {
        super(stub, nodeType);
    }

    @Nullable
    @Override
    public PsiElement getContext() {
        return RsExpandedElement.getContextImpl(this);
    }
}
