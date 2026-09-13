/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.lang.core.psi.ext.impl;

import consulo.language.psi.PsiElement;
import consulo.language.ast.ASTNode;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import org.rust.lang.core.psi.RsMacroBodyQuoteIdent;
import org.rust.lang.core.resolve.ref.RsMacroBodyReferenceDelegateImpl;
import org.rust.lang.core.resolve.ref.RsReference;
import org.rust.lang.core.psi.ext.*;

public abstract class RsMacroBodyQuoteIdentMixin extends RsElementImpl implements RsMacroBodyQuoteIdent {

    public RsMacroBodyQuoteIdentMixin(@Nonnull ASTNode node) {
        super(node);
    }

    @Nonnull
    @Override
    public PsiElement getReferenceNameElement() {
        return getQuoteIdentifier();
    }

    @Nullable
    @Override
    public RsReference getReference() {
        return new RsMacroBodyReferenceDelegateImpl(this);
    }
}
