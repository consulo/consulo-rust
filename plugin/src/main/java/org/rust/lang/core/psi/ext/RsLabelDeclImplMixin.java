/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.lang.core.psi.ext;

import consulo.language.psi.PsiElement;
import consulo.language.ast.ASTNode;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import org.rust.lang.core.psi.RsLabelDecl;
import org.rust.lang.core.psi.RsPsiFactory;

public abstract class RsLabelDeclImplMixin extends RsNamedElementImpl implements RsLabelDecl {

    public RsLabelDeclImplMixin(@Nonnull ASTNode type) {
        super(type);
    }

    @Nullable
    @Override
    public PsiElement getNameIdentifier() {
        return getQuoteIdentifier();
    }

    @Override
    public PsiElement setName(@Nonnull String name) {
        PsiElement id = getNameIdentifier();
        if (id != null) {
            id.replace(new RsPsiFactory(getProject()).createQuoteIdentifier(name));
        }
        return this;
    }
}
