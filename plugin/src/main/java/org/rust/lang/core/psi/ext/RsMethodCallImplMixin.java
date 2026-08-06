/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.lang.core.psi.ext;

import consulo.language.psi.PsiElement;
import consulo.language.ast.ASTNode;
import jakarta.annotation.Nonnull;
import org.rust.lang.core.psi.RsMethodCall;
import org.rust.lang.core.resolve.ref.RsMethodCallReferenceImpl;
import org.rust.lang.core.resolve.ref.RsReference;

public abstract class RsMethodCallImplMixin extends RsElementImpl implements RsMethodCall {
    public RsMethodCallImplMixin(@Nonnull ASTNode type) {
        super(type);
    }

    @Nonnull
    @Override
    public PsiElement getReferenceNameElement() {
        return getIdentifier();
    }

    @Nonnull
    @Override
    public RsReference getReference() {
        return new RsMethodCallReferenceImpl(this);
    }
}
