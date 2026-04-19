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
import org.rust.lang.core.psi.RsUnaryExpr;

public final class RsUnaryExprExtUtil {
    private RsUnaryExprExtUtil() {
    }

    public static boolean isDereference(@NotNull RsUnaryExpr expr) {
        return RsUnaryExprUtil.getOperatorType(expr) == UnaryOperator.DEREF;
    }

    @Nullable
    public static PsiElement getRaw(@NotNull RsUnaryExpr expr) {
        ASTNode child = expr.getNode().findChildByType(RsElementTypes.RAW);
        return child != null ? child.getPsi() : null;
    }
}
