/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.lang.core.psi.ext;

import com.intellij.psi.PsiElement;
import com.intellij.psi.tree.IElementType;
import org.jetbrains.annotations.NotNull;
import org.rust.lang.core.psi.RsMacroReference;
import org.rust.lang.core.resolve.ref.RsMacroReferenceImpl;
import org.rust.lang.core.resolve.ref.RsReference;

public abstract class RsMacroReferenceImplMixin extends RsElementImpl implements RsMacroReference {

    public RsMacroReferenceImplMixin(@NotNull IElementType type) {
        super(type);
    }

    @NotNull
    @Override
    public RsReference getReference() {
        return new RsMacroReferenceImpl(this);
    }

    @NotNull
    @Override
    public String getReferenceName() {
        return getReferenceNameElement().getText();
    }

    @NotNull
    @Override
    public PsiElement getReferenceNameElement() {
        return getMetaVarIdentifier();
    }
}
