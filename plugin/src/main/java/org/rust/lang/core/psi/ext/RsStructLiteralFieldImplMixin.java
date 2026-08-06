/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.lang.core.psi.ext;

import consulo.language.psi.PsiElement;
import consulo.language.ast.ASTNode;
import jakarta.annotation.Nonnull;
import org.rust.lang.core.psi.RsStructLiteralField;
import org.rust.lang.core.resolve.ref.RsReference;

public abstract class RsStructLiteralFieldImplMixin extends RsElementImpl implements RsStructLiteralField {

    public RsStructLiteralFieldImplMixin(@Nonnull ASTNode type) {
        super(type);
    }

    @Nonnull
    @Override
    public RsReference getReference() {
        return new org.rust.lang.core.resolve.ref.RsStructExprFieldReferenceImpl(this);
    }

    @Nonnull
    @Override
    public PsiElement getReferenceNameElement() {
        PsiElement id = getIdentifier();
        if (id != null) return id;
        PsiElement intLit = getIntegerLiteral();
        assert intLit != null;
        return intLit;
    }
}
