/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.lang.core.psi.ext.impl;

import consulo.language.ast.ASTNode;
import consulo.language.psi.stub.IStubElementType;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import org.rust.lang.core.psi.RsLitExpr;
import org.rust.lang.core.psi.RsMetaItem;
import org.rust.lang.core.stubs.RsMetaItemStub;
import org.rust.lang.core.psi.ext.*;

public abstract class RsMetaItemImplMixin extends RsStubbedElementImpl<RsMetaItemStub> implements RsMetaItem {
    public RsMetaItemImplMixin(@Nonnull ASTNode node) {
        super(node);
    }

    public RsMetaItemImplMixin(@Nonnull RsMetaItemStub stub, @Nonnull IStubElementType nodeType) {
        super(stub, nodeType);
    }

    /**
     * The attribute's name, e.g. {@code derive} in {@code #[derive(Clone)]}.
     * <p>
     * Declared here and not left to the {@link org.rust.lang.core.stubs.common.RsMetaItemPsiOrStub}
     * default: {@code PsiElementBase.getName()} returns {@code null}, and an inherited concrete
     * method takes precedence over an interface default, so without this override every attribute
     * would be nameless - which silently turns {@code #[derive(...)]} into an unrecognised attribute
     * procedural macro and makes the item it annotates disappear from resolve.
     */
    @Nullable
    @Override
    public String getName() {
        return RsMetaItemUtil.getName(this);
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
