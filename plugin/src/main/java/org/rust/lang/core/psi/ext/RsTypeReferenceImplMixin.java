/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.lang.core.psi.ext;

import consulo.language.ast.ASTNode;
import consulo.language.psi.PsiElement;
import consulo.language.psi.stub.IStubElementType;
import consulo.language.psi.stub.StubBase;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import org.rust.lang.core.macros.RsExpandedElement;
import org.rust.lang.core.psi.RsTypeReference;

public abstract class RsTypeReferenceImplMixin extends RsStubbedElementImpl<StubBase> implements RsTypeReference {

    public RsTypeReferenceImplMixin(@Nonnull ASTNode node) {
        super(node);
    }

    public RsTypeReferenceImplMixin(@Nonnull StubBase stub, @Nonnull IStubElementType nodeType) {
        super(stub, nodeType);
    }

    @Nullable
    @Override
    public PsiElement getContext() {
        return RsExpandedElement.getContextImpl(this);
    }
}
