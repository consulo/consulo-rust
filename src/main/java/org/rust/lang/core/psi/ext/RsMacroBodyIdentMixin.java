/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.lang.core.psi.ext;

import com.intellij.psi.PsiElement;
import com.intellij.psi.tree.IElementType;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.rust.lang.core.psi.RsMacroBodyIdent;
import org.rust.lang.core.resolve.ref.RsMacroBodyReferenceDelegateImpl;
import org.rust.lang.core.resolve.ref.RsReference;

public abstract class RsMacroBodyIdentMixin extends RsElementImpl implements RsMacroBodyIdent {

    public RsMacroBodyIdentMixin(@NotNull IElementType type) {
        super(type);
    }

    @NotNull
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
