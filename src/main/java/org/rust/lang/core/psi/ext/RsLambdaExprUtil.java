/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.lang.core.psi.ext;

import com.intellij.psi.PsiElement;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
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
    public static PsiElement getAsync(@NotNull RsLambdaExpr lambda) {
        com.intellij.lang.ASTNode node = lambda.getNode().findChildByType(RsElementTypes.ASYNC);
        return node != null ? node.getPsi() : null;
    }

    public static boolean isAsync(@NotNull RsLambdaExpr lambda) {
        return getAsync(lambda) != null;
    }

    public static boolean isConst(@NotNull RsLambdaExpr lambda) {
        return lambda.getNode().findChildByType(RsElementTypes.CONST) != null;
    }

    @NotNull
    public static List<RsValueParameter> getValueParameters(@NotNull RsLambdaExpr lambda) {
        return lambda.getValueParameterList().getValueParameterList();
    }

    @Nullable
    public static Ty getReturnType(@NotNull RsLambdaExpr lambda) {
        Ty type = ExtensionsUtil.getType(lambda);
        if (type instanceof TyFunctionBase) {
            return ((TyFunctionBase) type).getRetType();
        }
        return null;
    }
}
