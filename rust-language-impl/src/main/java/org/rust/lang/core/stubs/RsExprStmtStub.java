/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */
package org.rust.lang.core.stubs;

import consulo.language.ast.ASTNode;
import consulo.language.psi.stub.*;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import org.rust.lang.core.psi.RsExprStmt;
import java.io.IOException;
import org.rust.lang.core.psi.ext.impl.RsStmtUtil;
import org.rust.lang.core.psi.impl.RsExprStmtImpl;

public class RsExprStmtStub extends StubBase<RsExprStmt> {
    public final boolean hasSemicolon;

    public RsExprStmtStub(@Nullable StubElement parent, @Nonnull IStubElementType elementType, boolean hasSemicolon) {
        super(parent, elementType); this.hasSemicolon = hasSemicolon;
    }
    public boolean getHasSemicolon() { return hasSemicolon; }


    public static final RsStubElementType<RsExprStmtStub, RsExprStmt> Type =
        new RsStubElementType<RsExprStmtStub, RsExprStmt>("EXPR_STMT") {
            @Override public boolean shouldCreateStub(@Nonnull ASTNode node) { return StubImplementationsKt.shouldCreateStmtStub(node); }
            @Override public void serialize(@Nonnull RsExprStmtStub s, @Nonnull StubOutputStream ds) throws IOException {
                ds.writeBoolean(s.hasSemicolon);
            }
            @Nonnull @Override public RsExprStmtStub deserialize(@Nonnull StubInputStream ds, StubElement p) throws IOException {
                return new RsExprStmtStub(p, this, ds.readBoolean());
            }
            @Nonnull @Override public RsExprStmtStub createStub(@Nonnull RsExprStmt psi, @Nullable StubElement p) {
                return new RsExprStmtStub(p, this, RsStmtUtil.getHasSemicolon(psi));
            }
            @Nonnull @Override public RsExprStmt createPsi(@Nonnull RsExprStmtStub s) { return new RsExprStmtImpl(s, this); }
        };
}
