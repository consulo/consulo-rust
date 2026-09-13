/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.lang.core.psi.ext.impl;

import consulo.language.ast.ASTNode;
import consulo.language.psi.PsiElement;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import org.rust.lang.core.psi.RsElementTypes;
import org.rust.lang.core.psi.RsTypeAlias;
import org.rust.lang.core.psi.ext.*;

public final class RsTypeAliasUtil {
    private RsTypeAliasUtil() {
    }

    @Nullable
    public static PsiElement getDefault(@Nonnull RsTypeAlias typeAlias) {
        ASTNode child = typeAlias.getNode().findChildByType(RsElementTypes.DEFAULT);
        return child != null ? child.getPsi() : null;
    }
}
