/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.lang.core.stubs;

import consulo.language.ast.ASTNode;
import consulo.language.psi.stub.*;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import org.rust.lang.core.psi.RsUnaryExpr;
import org.rust.lang.core.psi.ext.impl.UnaryOperator;

import java.io.IOException;
import org.rust.lang.core.psi.ext.impl.RsUnaryExprUtil;
import org.rust.lang.core.psi.impl.RsUnaryExprImpl;

public final class RsUnaryExprStub extends RsPlaceholderStub<RsUnaryExpr> {
    @Nonnull
    private final UnaryOperator operatorType;

    public RsUnaryExprStub(@Nullable StubElement parent, @Nonnull IStubElementType elementType, @Nonnull UnaryOperator operatorType) {
        super(parent, elementType);
        this.operatorType = operatorType;
    }

    @Nonnull
    public UnaryOperator getOperatorType() {
        return operatorType;
    }


    public static final RsStubElementType<RsUnaryExprStub, RsUnaryExpr> Type =
        new RsStubElementType<RsUnaryExprStub, RsUnaryExpr>("UNARY_EXPR") {
            @Override
            public boolean shouldCreateStub(@Nonnull ASTNode node) {
                return StubImplementationsKt.shouldCreateExprStub(node);
            }

            @Override
            public void serialize(@Nonnull RsUnaryExprStub stub, @Nonnull StubOutputStream dataStream) throws IOException {
                dataStream.writeUTFFast(stub.getOperatorType().name());
            }

            @Nonnull
            @Override
            public RsUnaryExprStub deserialize(@Nonnull StubInputStream dataStream, StubElement parentStub) throws IOException {
                String name = dataStream.readUTFFast();
                UnaryOperator op = UnaryOperator.valueOf(name);
                return new RsUnaryExprStub(parentStub, this, op);
            }

            @Nonnull
            @Override
            public RsUnaryExpr createPsi(@Nonnull RsUnaryExprStub stub) {
                return new RsUnaryExprImpl(stub, this);
            }

            @Nonnull
            @Override
            public RsUnaryExprStub createStub(@Nonnull RsUnaryExpr psi, @Nullable StubElement parentStub) {
                UnaryOperator op = RsUnaryExprUtil.getOperatorType(psi);
                return new RsUnaryExprStub(parentStub, this, op);
            }
        };
}
