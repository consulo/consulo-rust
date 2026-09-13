/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */
package org.rust.lang.core.stubs;

import consulo.language.ast.ASTNode;
import consulo.language.psi.stub.*;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import org.rust.lang.core.psi.RsBinaryOp;
import java.io.IOException;
import org.rust.lang.core.psi.impl.RsBinaryOpImpl;

public class RsBinaryOpStub extends StubBase<RsBinaryOp> {
    @Nonnull private final String op;

    public RsBinaryOpStub(@Nullable StubElement parent, @Nonnull IStubElementType elementType, @Nonnull String op) {
        super(parent, elementType); this.op = op;
    }
    @Nonnull public String getOp() { return op; }

    public static final RsStubElementType<RsBinaryOpStub, RsBinaryOp> Type =
        new RsStubElementType<RsBinaryOpStub, RsBinaryOp>("BINARY_OP") {
            @Override public boolean shouldCreateStub(@Nonnull ASTNode node) { return createStubIfParentIsStub(node); }
            @Override public void serialize(@Nonnull RsBinaryOpStub s, @Nonnull StubOutputStream ds) throws IOException {
                ds.writeUTFFast(s.op);
            }
            @Nonnull @Override public RsBinaryOpStub deserialize(@Nonnull StubInputStream ds, StubElement p) throws IOException {
                return new RsBinaryOpStub(p, this, ds.readUTFFast());
            }
            @Nonnull @Override public RsBinaryOpStub createStub(@Nonnull RsBinaryOp psi, @Nullable StubElement p) {
                return new RsBinaryOpStub(p, this, psi.getText());
            }
            @Nonnull @Override public RsBinaryOp createPsi(@Nonnull RsBinaryOpStub s) { return new RsBinaryOpImpl(s, this); }
        };
}
