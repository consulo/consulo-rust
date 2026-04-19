/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.lang.core.psi.ext;

import com.intellij.psi.PsiElement;
import com.intellij.psi.tree.IElementType;
import org.jetbrains.annotations.NotNull;
import org.rust.lang.core.macros.RsExpandedElement;
import org.rust.lang.core.psi.RsPat;

public abstract class RsPatImplMixin extends RsElementImpl implements RsPat {
    public RsPatImplMixin(@NotNull IElementType type) {
        super(type);
    }

    @Override
    public PsiElement getContext() {
        return RsExpandedElement.getContextImpl(this);
    }
}
