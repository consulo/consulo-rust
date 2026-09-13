/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.lang.core.psi.ext.impl;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import org.rust.lang.core.psi.RsArrayType;
import org.rust.lang.core.psi.RsExpr;
import org.rust.lang.core.stubs.RsArrayTypeStub;
import org.rust.lang.core.types.consts.CtValue;
import org.rust.lang.core.types.ty.TyInteger;
import org.rust.lang.utils.evaluation.ConstExprEvaluator;
import org.rust.lang.core.psi.ext.*;

public final class RsArrayTypeUtil {

    private RsArrayTypeUtil() {
    }

    public static boolean isSlice(@Nonnull RsArrayType type) {
        Object stub = RsPsiJavaUtil.getGreenStub(type);
        if (stub instanceof RsArrayTypeStub) {
            return ((RsArrayTypeStub) stub).isSlice();
        }
        return type.getExpr() == null;
    }

    @Nullable
    public static Long getArraySize(@Nonnull RsArrayType type) {
        RsExpr expr = type.getExpr();
        if (expr == null) return null;
        return CtValue.asLong(ConstExprEvaluator.evaluate(expr, TyInteger.USize.INSTANCE));
    }
}
