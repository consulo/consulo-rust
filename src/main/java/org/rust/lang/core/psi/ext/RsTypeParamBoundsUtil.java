/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.lang.core.psi.ext;

import com.intellij.lang.ASTNode;
import com.intellij.psi.PsiElement;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.rust.lang.core.psi.RsElementTypes;
import org.rust.lang.core.psi.RsTypeParamBounds;

public final class RsTypeParamBoundsUtil {
    private RsTypeParamBoundsUtil() {
    }

    @Nullable
    public static PsiElement getDyn(@NotNull RsTypeParamBounds bounds) {
        ASTNode child = bounds.getNode().findChildByType(RsElementTypes.DYN);
        return child != null ? child.getPsi() : null;
    }
}
