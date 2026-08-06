/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.lang.core.psi.ext;

import consulo.language.psi.PsiElement;
import consulo.language.ast.ASTNode;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import org.rust.lang.core.psi.RsMacroBodyIdent;
import org.rust.lang.core.resolve.ref.RsMacroBodyReferenceDelegateImpl;
import org.rust.lang.core.resolve.ref.RsReference;

public abstract class RsMacroBodyIdentMixin extends RsElementImpl implements RsMacroBodyIdent {

    public RsMacroBodyIdentMixin(@Nonnull ASTNode node) {
        super(node);
    }

    @Nonnull
    @Override
    public PsiElement getReferenceNameElement() {
        return getIdentifier();
    }

    @Nullable
    @Override
    public RsReference getReference() {
        return new RsMacroBodyReferenceDelegateImpl(this);
    }
}
