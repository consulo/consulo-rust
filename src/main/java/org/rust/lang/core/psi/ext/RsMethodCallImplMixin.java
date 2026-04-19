/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.lang.core.psi.ext;

import com.intellij.psi.PsiElement;
import com.intellij.psi.tree.IElementType;
import org.jetbrains.annotations.NotNull;
import org.rust.lang.core.psi.RsMethodCall;
import org.rust.lang.core.resolve.ref.RsMethodCallReferenceImpl;
import org.rust.lang.core.resolve.ref.RsReference;

public abstract class RsMethodCallImplMixin extends RsElementImpl implements RsMethodCall {
    public RsMethodCallImplMixin(@NotNull IElementType type) {
        super(type);
    }

    @NotNull
    @Override
    public PsiElement getReferenceNameElement() {
        return getIdentifier();
    }

    @NotNull
    @Override
    public RsReference getReference() {
        return new RsMethodCallReferenceImpl(this);
    }
}
