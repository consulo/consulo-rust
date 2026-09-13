/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.lang.core.psi.ext.impl;

import consulo.language.psi.PsiElement;
import consulo.language.ast.ASTNode;
import jakarta.annotation.Nonnull;
import org.rust.lang.core.macros.RsExpandedElement;
import org.rust.lang.core.psi.RsPat;
import org.rust.lang.core.psi.ext.*;

public abstract class RsPatImplMixin extends RsElementImpl implements RsPat {
    public RsPatImplMixin(@Nonnull ASTNode type) {
        super(type);
    }

    @Override
    public PsiElement getContext() {
        return RsExpandedElement.getContextImpl(this);
    }
}
