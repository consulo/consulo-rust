/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */
package org.rust.lang.core.stubs;

import com.intellij.lang.ASTNode;
import com.intellij.psi.stubs.*;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.rust.lang.core.psi.RsBinaryOp;
import org.rust.lang.core.psi.impl.RsBinaryOpImpl;
import java.io.IOException;

public class RsBinaryOpStub extends StubBase<RsBinaryOp> {
    @NotNull private final String op;

    public RsBinaryOpStub(@Nullable StubElement<?> parent, @NotNull IStubElementType<?, ?> elementType, @NotNull String op) {
        super(parent, elementType); this.op = op;
    }
    @NotNull public String getOp() { return op; }

    public static final RsStubElementType<RsBinaryOpStub, RsBinaryOp> Type =
        new RsStubElementType<RsBinaryOpStub, RsBinaryOp>("BINARY_OP") {
            @Override public boolean shouldCreateStub(@NotNull ASTNode node) { return createStubIfParentIsStub(node); }
            @Override public void serialize(@NotNull RsBinaryOpStub s, @NotNull StubOutputStream ds) throws IOException {
                ds.writeUTFFast(s.op);
            }
            @NotNull @Override public RsBinaryOpStub deserialize(@NotNull StubInputStream ds, StubElement p) throws IOException {
                return new RsBinaryOpStub(p, this, ds.readUTFFast());
            }
            @NotNull @Override public RsBinaryOpStub createStub(@NotNull RsBinaryOp psi, @Nullable StubElement<?> p) {
                return new RsBinaryOpStub(p, this, psi.getText());
            }
            @NotNull @Override public RsBinaryOp createPsi(@NotNull RsBinaryOpStub s) { return new RsBinaryOpImpl(s, this); }
        };
}
