/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.lang.core.stubs;

import com.intellij.lang.ASTNode;
import com.intellij.psi.stubs.*;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.rust.lang.core.psi.RsUnaryExpr;
import org.rust.lang.core.psi.ext.UnaryOperator;
import org.rust.lang.core.psi.impl.RsUnaryExprImpl;

import java.io.IOException;
import org.rust.lang.core.psi.ext.RsUnaryExprUtil;

public final class RsUnaryExprStub extends RsPlaceholderStub<RsUnaryExpr> {
    @NotNull
    private final UnaryOperator operatorType;

    public RsUnaryExprStub(@Nullable StubElement<?> parent, @NotNull IStubElementType<?, ?> elementType, @NotNull UnaryOperator operatorType) {
        super(parent, elementType);
        this.operatorType = operatorType;
    }

    @NotNull
    public UnaryOperator getOperatorType() {
        return operatorType;
    }

    public static final RsStubElementType<RsUnaryExprStub, RsUnaryExpr> Type =
        new RsStubElementType<RsUnaryExprStub, RsUnaryExpr>("UNARY_EXPR") {
            @Override
            public boolean shouldCreateStub(@NotNull ASTNode node) {
                return StubImplementationsKt.shouldCreateExprStub(node);
            }

            @Override
            public void serialize(@NotNull RsUnaryExprStub stub, @NotNull StubOutputStream dataStream) throws IOException {
                dataStream.writeUTFFast(stub.getOperatorType().name());
            }

            @NotNull
            @Override
            public RsUnaryExprStub deserialize(@NotNull StubInputStream dataStream, StubElement parentStub) throws IOException {
                String name = dataStream.readUTFFast();
                UnaryOperator op = UnaryOperator.valueOf(name);
                return new RsUnaryExprStub(parentStub, this, op);
            }

            @NotNull
            @Override
            public RsUnaryExpr createPsi(@NotNull RsUnaryExprStub stub) {
                return new RsUnaryExprImpl(stub, this);
            }

            @NotNull
            @Override
            public RsUnaryExprStub createStub(@NotNull RsUnaryExpr psi, @Nullable StubElement<?> parentStub) {
                UnaryOperator op = RsUnaryExprUtil.getOperatorType(psi);
                return new RsUnaryExprStub(parentStub, this, op);
            }
        };
}
