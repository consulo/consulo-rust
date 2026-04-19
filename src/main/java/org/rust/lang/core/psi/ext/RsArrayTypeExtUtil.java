/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.lang.core.psi.ext;

import com.intellij.extapi.psi.StubBasedPsiElementBase;
import com.intellij.psi.stubs.StubElement;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.rust.lang.core.psi.RsArrayType;
import org.rust.lang.core.psi.RsExpr;
import org.rust.lang.core.stubs.RsArrayTypeStub;
import org.rust.lang.core.types.consts.CtValue;
import org.rust.lang.core.types.ty.TyInteger;
import org.rust.lang.utils.evaluation.ConstExprEvaluator;

/**
 * Extension functions for {@link RsArrayType}.
 */
public final class RsArrayTypeExtUtil {

    private RsArrayTypeExtUtil() {
    }

    public static boolean isSlice(@NotNull RsArrayType type) {
        if (type instanceof StubBasedPsiElementBase) {
            StubElement<?> stub = ((StubBasedPsiElementBase<?>) type).getGreenStub();
            if (stub instanceof RsArrayTypeStub) {
                return ((RsArrayTypeStub) stub).isSlice();
            }
        }
        return type.getExpr() == null;
    }

    @Nullable
    public static Long getArraySize(@NotNull RsArrayType type) {
        RsExpr expr = type.getExpr();
        if (expr == null) return null;
        return CtValue.asLong(ConstExprEvaluator.evaluate(expr, TyInteger.USize.INSTANCE));
    }
}
