/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.lang.core.psi.ext.impl;

import consulo.language.impl.psi.stub.StubBasedPsiElementBase;
import consulo.language.psi.stub.StubElement;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import org.rust.lang.core.psi.RsBlock;
import org.rust.lang.core.psi.RsBlockExpr;
import org.rust.lang.core.psi.RsElementTypes;
import org.rust.lang.core.psi.RsExpr;
import org.rust.lang.core.stubs.RsBlockExprStub;
import org.rust.lang.core.psi.ext.*;

/**
 * Extension functions for {@link RsBlockExpr}.
 */
public final class RsBlockExprUtil {

    private RsBlockExprUtil() {
    }

    @Nullable
    private static RsBlockExprStub getBlockExprStub(@Nonnull RsBlockExpr expr) {
        if (expr instanceof StubBasedPsiElementBase) {
            StubElement<?> stub = ((StubBasedPsiElementBase<?>) expr).getGreenStub();
            if (stub instanceof RsBlockExprStub) {
                return (RsBlockExprStub) stub;
            }
        }
        return null;
    }

    public static boolean isUnsafe(@Nonnull RsBlockExpr expr) {
        RsBlockExprStub stub = getBlockExprStub(expr);
        if (stub != null) return stub.isUnsafe();
        return expr.getNode().findChildByType(RsElementTypes.UNSAFE) != null;
    }

    public static boolean isAsync(@Nonnull RsBlockExpr expr) {
        RsBlockExprStub stub = getBlockExprStub(expr);
        if (stub != null) return stub.isAsync();
        return expr.getNode().findChildByType(RsElementTypes.ASYNC) != null;
    }

    public static boolean isTry(@Nonnull RsBlockExpr expr) {
        RsBlockExprStub stub = getBlockExprStub(expr);
        if (stub != null) return stub.isTry();
        return expr.getNode().findChildByType(RsElementTypes.TRY) != null;
    }

    public static boolean isConst(@Nonnull RsBlockExpr expr) {
        RsBlockExprStub stub = getBlockExprStub(expr);
        if (stub != null) return stub.isConst();
        return expr.getNode().findChildByType(RsElementTypes.CONST) != null;
    }

    /**
     * Convenience: get the expanded tail expression from the block.
     */
    @Nullable
    public static RsExpr getExpandedTailExpr(@Nonnull RsBlock block) {
        return RsBlockUtil.getExpandedTailExpr(block);
    }
}
