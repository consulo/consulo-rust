/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.lang.core.psi.ext;

import com.intellij.psi.PsiElement;
import com.intellij.psi.tree.IElementType;
import org.jetbrains.annotations.NotNull;
import org.rust.lang.core.psi.RsLabel;
import org.rust.lang.core.resolve.ref.RsLabelReferenceImpl;
import org.rust.lang.core.resolve.ref.RsReference;

public abstract class RsLabelImplMixin extends RsElementImpl implements RsLabel {

    public RsLabelImplMixin(@NotNull IElementType type) {
        super(type);
    }

    @NotNull
    @Override
    public PsiElement getReferenceNameElement() {
        return getQuoteIdentifier();
    }

    @NotNull
    @Override
    public RsReference getReference() {
        return new RsLabelReferenceImpl(this);
    }
}
