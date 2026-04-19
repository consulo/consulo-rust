/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.lang.core.psi.ext;

import com.intellij.extapi.psi.StubBasedPsiElementBase;
import com.intellij.psi.stubs.StubElement;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.rust.lang.core.psi.RsBlock;
import org.rust.lang.core.psi.RsBlockExpr;
import org.rust.lang.core.psi.RsElementTypes;
import org.rust.lang.core.psi.RsExpr;
import org.rust.lang.core.stubs.RsBlockExprStub;

/**
 * Extension functions for {@link RsBlockExpr}.
 */
public final class RsBlockExprUtil {

    private RsBlockExprUtil() {
    }

    @Nullable
    private static RsBlockExprStub getBlockExprStub(@NotNull RsBlockExpr expr) {
        if (expr instanceof StubBasedPsiElementBase) {
            StubElement<?> stub = ((StubBasedPsiElementBase<?>) expr).getGreenStub();
            if (stub instanceof RsBlockExprStub) {
                return (RsBlockExprStub) stub;
            }
        }
        return null;
    }

    public static boolean isUnsafe(@NotNull RsBlockExpr expr) {
        RsBlockExprStub stub = getBlockExprStub(expr);
        if (stub != null) return stub.isUnsafe();
        return expr.getNode().findChildByType(RsElementTypes.UNSAFE) != null;
    }

    public static boolean isAsync(@NotNull RsBlockExpr expr) {
        RsBlockExprStub stub = getBlockExprStub(expr);
        if (stub != null) return stub.isAsync();
        return expr.getNode().findChildByType(RsElementTypes.ASYNC) != null;
    }

    public static boolean isTry(@NotNull RsBlockExpr expr) {
        RsBlockExprStub stub = getBlockExprStub(expr);
        if (stub != null) return stub.isTry();
        return expr.getNode().findChildByType(RsElementTypes.TRY) != null;
    }

    public static boolean isConst(@NotNull RsBlockExpr expr) {
        RsBlockExprStub stub = getBlockExprStub(expr);
        if (stub != null) return stub.isConst();
        return expr.getNode().findChildByType(RsElementTypes.CONST) != null;
    }

    /**
     * Convenience: get the expanded tail expression from the block.
     */
    @Nullable
    public static RsExpr getExpandedTailExpr(@NotNull RsBlock block) {
        return RsBlockUtil.getExpandedTailExpr(block);
    }
}
