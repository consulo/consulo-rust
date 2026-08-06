/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.lang.core.psi.ext;

import consulo.language.ast.ASTNode;
import consulo.language.psi.PsiElement;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import org.rust.lang.core.psi.RsElementTypes;
import org.rust.lang.core.psi.RsTypeParamBounds;

public final class RsTypeParamBoundsUtil {
    private RsTypeParamBoundsUtil() {
    }

    @Nullable
    public static PsiElement getDyn(@Nonnull RsTypeParamBounds bounds) {
        ASTNode child = bounds.getNode().findChildByType(RsElementTypes.DYN);
        return child != null ? child.getPsi() : null;
    }
}
