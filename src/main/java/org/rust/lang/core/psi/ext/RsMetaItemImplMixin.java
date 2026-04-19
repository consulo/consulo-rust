/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.lang.core.psi.ext;

import com.intellij.lang.ASTNode;
import com.intellij.psi.stubs.IStubElementType;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.rust.lang.core.psi.RsLitExpr;
import org.rust.lang.core.psi.RsMetaItem;
import org.rust.lang.core.stubs.RsMetaItemStub;

public abstract class RsMetaItemImplMixin extends RsStubbedElementImpl<RsMetaItemStub> implements RsMetaItem {
    public RsMetaItemImplMixin(@NotNull ASTNode node) {
        super(node);
    }

    public RsMetaItemImplMixin(@NotNull RsMetaItemStub stub, @NotNull IStubElementType<?, ?> nodeType) {
        super(stub, nodeType);
    }

    @Override
    public boolean getHasEq() {
        RsMetaItemStub stub = getStub();
        return stub != null ? stub.getHasEq() : getEq() != null;
    }

    @Nullable
    @Override
    public String getValue() {
        RsLitExpr lit = getLitExpr();
        return lit != null ? RsLitExprUtil.getStringValue(lit) : null;
    }
}
