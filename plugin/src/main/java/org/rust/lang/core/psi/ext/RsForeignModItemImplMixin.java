/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.lang.core.psi.ext;

import consulo.language.ast.ASTNode;
import consulo.language.psi.PsiElement;
import consulo.language.psi.stub.IStubElementType;
import jakarta.annotation.Nonnull;
import org.rust.lang.core.macros.RsExpandedElement;
import org.rust.lang.core.psi.RsForeignModItem;
import org.rust.lang.core.stubs.RsForeignModStub;

public abstract class RsForeignModItemImplMixin extends RsStubbedElementImpl<RsForeignModStub>
    implements RsForeignModItem {

    public RsForeignModItemImplMixin(@Nonnull ASTNode node) {
        super(node);
    }

    public RsForeignModItemImplMixin(@Nonnull RsForeignModStub stub, @Nonnull IStubElementType elementType) {
        super(stub, elementType);
    }

    @Nonnull
    @Override
    public RsVisibility getVisibility() {
        return RsVisibility.Private.INSTANCE;
    }

    @Override
    public PsiElement getContext() {
        return RsExpandedElement.getContextImpl(this);
    }
}
