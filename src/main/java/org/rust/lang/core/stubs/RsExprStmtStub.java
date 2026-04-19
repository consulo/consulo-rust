/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */
package org.rust.lang.core.stubs;

import com.intellij.lang.ASTNode;
import com.intellij.psi.stubs.*;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.rust.lang.core.psi.RsExprStmt;
import org.rust.lang.core.psi.impl.RsExprStmtImpl;
import java.io.IOException;
import org.rust.lang.core.psi.ext.RsStmtUtil;

public class RsExprStmtStub extends StubBase<RsExprStmt> {
    private final boolean hasSemicolon;

    public RsExprStmtStub(@Nullable StubElement<?> parent, @NotNull IStubElementType<?, ?> elementType, boolean hasSemicolon) {
        super(parent, elementType); this.hasSemicolon = hasSemicolon;
    }
    public boolean getHasSemicolon() { return hasSemicolon; }

    public static final RsStubElementType<RsExprStmtStub, RsExprStmt> Type =
        new RsStubElementType<RsExprStmtStub, RsExprStmt>("EXPR_STMT") {
            @Override public boolean shouldCreateStub(@NotNull ASTNode node) { return StubImplementationsKt.shouldCreateStmtStub(node); }
            @Override public void serialize(@NotNull RsExprStmtStub s, @NotNull StubOutputStream ds) throws IOException {
                ds.writeBoolean(s.hasSemicolon);
            }
            @NotNull @Override public RsExprStmtStub deserialize(@NotNull StubInputStream ds, StubElement p) throws IOException {
                return new RsExprStmtStub(p, this, ds.readBoolean());
            }
            @NotNull @Override public RsExprStmtStub createStub(@NotNull RsExprStmt psi, @Nullable StubElement<?> p) {
                return new RsExprStmtStub(p, this, RsStmtUtil.getHasSemicolon(psi));
            }
            @NotNull @Override public RsExprStmt createPsi(@NotNull RsExprStmtStub s) { return new RsExprStmtImpl(s, this); }
        };
}
