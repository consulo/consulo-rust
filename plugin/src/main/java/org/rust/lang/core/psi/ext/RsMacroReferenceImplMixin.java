/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.lang.core.psi.ext;

import consulo.language.psi.PsiElement;
import consulo.language.ast.ASTNode;
import jakarta.annotation.Nonnull;
import org.rust.lang.core.psi.RsMacroReference;
import org.rust.lang.core.resolve.ref.RsMacroReferenceImpl;
import org.rust.lang.core.resolve.ref.RsReference;

public abstract class RsMacroReferenceImplMixin extends RsElementImpl implements RsMacroReference {

    public RsMacroReferenceImplMixin(@Nonnull ASTNode type) {
        super(type);
    }

    @Nonnull
    @Override
    public RsReference getReference() {
        return new RsMacroReferenceImpl(this);
    }

    @Nonnull
    @Override
    public String getReferenceName() {
        return getReferenceNameElement().getText();
    }

    @Nonnull
    @Override
    public PsiElement getReferenceNameElement() {
        return getMetaVarIdentifier();
    }
}
