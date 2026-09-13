/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.lang.core.psi.ext.impl;

import consulo.language.ast.ASTNode;
import consulo.language.psi.PsiElement;
import consulo.language.psi.stub.IStubElementType;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import org.rust.lang.core.psi.RsAlias;
import org.rust.lang.core.stubs.RsAliasStub;
import org.rust.lang.core.psi.ext.*;

public abstract class RsAliasImplMixin extends RsStubbedNamedElementImpl<RsAliasStub> implements RsAlias {

    public RsAliasImplMixin(@Nonnull ASTNode node) {
        super(node);
    }

    public RsAliasImplMixin(@Nonnull RsAliasStub stub, @Nonnull IStubElementType elementType) {
        super(stub, elementType);
    }

    @Nullable
    @Override
    public PsiElement getNameIdentifier() {
        // `use Foo as _;` really should be unnamed, but "_" is not a valid name in rust, so I think it's ok
        PsiElement id = getIdentifier();
        if (id != null) return id;
        return getUnderscore();
    }
}
