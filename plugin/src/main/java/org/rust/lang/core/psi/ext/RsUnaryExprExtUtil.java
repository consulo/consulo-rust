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
import org.rust.lang.core.psi.RsUnaryExpr;

public final class RsUnaryExprExtUtil {
    private RsUnaryExprExtUtil() {
    }

    public static boolean isDereference(@Nonnull RsUnaryExpr expr) {
        return RsUnaryExprUtil.getOperatorType(expr) == UnaryOperator.DEREF;
    }

    @Nullable
    public static PsiElement getRaw(@Nonnull RsUnaryExpr expr) {
        ASTNode child = expr.getNode().findChildByType(RsElementTypes.RAW);
        return child != null ? child.getPsi() : null;
    }
}
