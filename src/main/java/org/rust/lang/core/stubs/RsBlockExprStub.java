/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.lang.core.stubs;

import com.intellij.lang.ASTNode;
import com.intellij.psi.stubs.*;
import com.intellij.util.BitUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.rust.lang.core.psi.RsBlockExpr;
import org.rust.lang.core.psi.impl.RsBlockExprImpl;
import org.rust.stdext.BitFlagsBuilder;

import java.io.IOException;
import org.rust.lang.core.psi.ext.RsBlockExprUtil;

public class RsBlockExprStub extends RsPlaceholderStub<RsBlockExpr> {
    private final int flags;

    public RsBlockExprStub(@Nullable StubElement<?> parent, @NotNull IStubElementType<?, ?> elementType, int flags) {
        super(parent, elementType);
        this.flags = flags;
    }

    public boolean isUnsafe() {
        return BitUtil.isSet(flags, UNSAFE_MASK);
    }

    public boolean isAsync() {
        return BitUtil.isSet(flags, ASYNC_MASK);
    }

    public boolean isTry() {
        return BitUtil.isSet(flags, TRY_MASK);
    }

    public boolean isConst() {
        return BitUtil.isSet(flags, CONST_MASK);
    }

    private static final Flags FLAGS = new Flags();
    private static final int UNSAFE_MASK = FLAGS.nextMask();
    private static final int ASYNC_MASK = FLAGS.nextMask();
    private static final int TRY_MASK = FLAGS.nextMask();
    private static final int CONST_MASK = FLAGS.nextMask();

    private static class Flags extends BitFlagsBuilder {
        Flags() {
            super(Limit.BYTE);
        }

        int nextMask() {
            return nextBitMask();
        }
    }

    public static final RsStubElementType<RsBlockExprStub, RsBlockExpr> Type =
        new RsStubElementType<RsBlockExprStub, RsBlockExpr>("BLOCK_EXPR") {
            @Override
            public boolean shouldCreateStub(@NotNull ASTNode node) {
                return StubImplementationsKt.shouldCreateExprStub(node);
            }

            @Override
            public void serialize(@NotNull RsBlockExprStub stub, @NotNull StubOutputStream dataStream) throws IOException {
                dataStream.writeInt(stub.flags);
            }

            @NotNull
            @Override
            public RsBlockExprStub deserialize(@NotNull StubInputStream dataStream, StubElement parentStub) throws IOException {
                return new RsBlockExprStub(parentStub, this, dataStream.readInt());
            }

            @NotNull
            @Override
            public RsBlockExprStub createStub(@NotNull RsBlockExpr psi, @Nullable StubElement<?> parentStub) {
                int flags = 0;
                flags = BitUtil.set(flags, UNSAFE_MASK, RsBlockExprUtil.isUnsafe(psi));
                flags = BitUtil.set(flags, ASYNC_MASK, RsBlockExprUtil.isAsync(psi));
                flags = BitUtil.set(flags, TRY_MASK, RsBlockExprUtil.isTry(psi));
                flags = BitUtil.set(flags, CONST_MASK, RsBlockExprUtil.isConst(psi));
                return new RsBlockExprStub(parentStub, this, flags);
            }

            @NotNull
            @Override
            public RsBlockExpr createPsi(@NotNull RsBlockExprStub stub) {
                return new RsBlockExprImpl(stub, this);
            }
        };
}
