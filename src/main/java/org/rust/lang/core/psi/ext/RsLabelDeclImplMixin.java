/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.lang.core.psi.ext;

import com.intellij.psi.PsiElement;
import com.intellij.psi.tree.IElementType;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.rust.lang.core.psi.RsLabelDecl;
import org.rust.lang.core.psi.RsPsiFactory;

public abstract class RsLabelDeclImplMixin extends RsNamedElementImpl implements RsLabelDecl {

    public RsLabelDeclImplMixin(@NotNull IElementType type) {
        super(type);
    }

    @Nullable
    @Override
    public PsiElement getNameIdentifier() {
        return getQuoteIdentifier();
    }

    @Override
    public PsiElement setName(@NotNull String name) {
        PsiElement id = getNameIdentifier();
        if (id != null) {
            id.replace(new RsPsiFactory(getProject()).createQuoteIdentifier(name));
        }
        return this;
    }
}
