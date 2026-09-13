/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.lang.core.psi.ext.impl;

import consulo.language.psi.PsiElement;
import consulo.language.ast.ASTNode;
import jakarta.annotation.Nonnull;
import org.rust.lang.core.psi.RsLabel;
import org.rust.lang.core.resolve.ref.RsLabelReferenceImpl;
import org.rust.lang.core.resolve.ref.RsReference;
import org.rust.lang.core.psi.ext.*;

public abstract class RsLabelImplMixin extends RsElementImpl implements RsLabel {

    public RsLabelImplMixin(@Nonnull ASTNode type) {
        super(type);
    }

    @Nonnull
    @Override
    public PsiElement getReferenceNameElement() {
        return getQuoteIdentifier();
    }

    @Nonnull
    @Override
    public RsReference getReference() {
        return new RsLabelReferenceImpl(this);
    }
}
