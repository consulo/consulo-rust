/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.lang.core.psi.ext;

import consulo.language.psi.PsiElement;
import consulo.language.ast.ASTNode;
import jakarta.annotation.Nonnull;
import org.rust.lang.core.psi.RsFieldLookup;
import org.rust.lang.core.resolve.ref.RsFieldLookupReferenceImpl;
import org.rust.lang.core.resolve.ref.RsReference;

public abstract class RsFieldLookupImplMixin extends RsElementImpl implements RsFieldLookup {

    public RsFieldLookupImplMixin(@Nonnull ASTNode type) {
        super(type);
    }

    @Nonnull
    @Override
    public PsiElement getReferenceNameElement() {
        PsiElement id = getIdentifier();
        if (id != null) return id;
        PsiElement intLit = getIntegerLiteral();
        if (intLit != null) return intLit;
        throw new IllegalStateException("RsFieldLookup must have identifier or integer literal");
    }

    @Nonnull
    @Override
    public RsReference getReference() {
        return new RsFieldLookupReferenceImpl(this);
    }
}
