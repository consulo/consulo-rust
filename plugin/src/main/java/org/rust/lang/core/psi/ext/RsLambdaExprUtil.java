/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.lang.core.psi.ext;

import consulo.language.psi.PsiElement;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import org.rust.lang.core.psi.RsElementTypes;
import org.rust.lang.core.psi.RsLambdaExpr;
import org.rust.lang.core.psi.RsValueParameter;
import org.rust.lang.core.types.ExtensionsUtil;
import org.rust.lang.core.types.ty.Ty;
import org.rust.lang.core.types.ty.TyFunctionBase;

import java.util.List;

public final class RsLambdaExprUtil {
    private RsLambdaExprUtil() {
    }

    @Nullable
    public static PsiElement getAsync(@Nonnull RsLambdaExpr lambda) {
        consulo.language.ast.ASTNode node = lambda.getNode().findChildByType(RsElementTypes.ASYNC);
        return node != null ? node.getPsi() : null;
    }

    public static boolean isAsync(@Nonnull RsLambdaExpr lambda) {
        return getAsync(lambda) != null;
    }

    public static boolean isConst(@Nonnull RsLambdaExpr lambda) {
        return lambda.getNode().findChildByType(RsElementTypes.CONST) != null;
    }

    @Nonnull
    public static List<RsValueParameter> getValueParameters(@Nonnull RsLambdaExpr lambda) {
        return lambda.getValueParameterList().getValueParameterList();
    }

    @Nullable
    public static Ty getReturnType(@Nonnull RsLambdaExpr lambda) {
        Ty type = ExtensionsUtil.getType(lambda);
        if (type instanceof TyFunctionBase) {
            return ((TyFunctionBase) type).getRetType();
        }
        return null;
    }
}
