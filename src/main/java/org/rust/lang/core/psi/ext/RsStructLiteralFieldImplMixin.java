/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.lang.core.psi.ext;

import com.intellij.psi.PsiElement;
import com.intellij.psi.tree.IElementType;
import org.jetbrains.annotations.NotNull;
import org.rust.lang.core.psi.RsStructLiteralField;
import org.rust.lang.core.resolve.ref.RsReference;

public abstract class RsStructLiteralFieldImplMixin extends RsElementImpl implements RsStructLiteralField {

    public RsStructLiteralFieldImplMixin(@NotNull IElementType type) {
        super(type);
    }

    @NotNull
    @Override
    public RsReference getReference() {
        return new org.rust.lang.core.resolve.ref.RsStructExprFieldReferenceImpl(this);
    }

    @NotNull
    @Override
    public PsiElement getReferenceNameElement() {
        PsiElement id = getIdentifier();
        if (id != null) return id;
        PsiElement intLit = getIntegerLiteral();
        assert intLit != null;
        return intLit;
    }
}
