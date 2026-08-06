/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */
package org.rust.lang.core.stubs;

import consulo.language.ast.ASTNode;
import consulo.language.psi.stub.*;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import org.rust.lang.core.psi.RsLetDecl;
import org.rust.lang.core.psi.impl.RsLetDeclImpl;
import java.io.IOException;

public class RsLetDeclStub extends StubBase<RsLetDecl> {

    public RsLetDeclStub(@Nullable StubElement parent, @Nonnull IStubElementType elementType) {
        super(parent, elementType);
    }

    public static final RsStubElementType<RsLetDeclStub, RsLetDecl> Type =
        new RsStubElementType<RsLetDeclStub, RsLetDecl>("LET_DECL") {
            @Override public boolean shouldCreateStub(@Nonnull ASTNode node) { return StubImplementationsKt.shouldCreateStmtStub(node); }
            @Override public void serialize(@Nonnull RsLetDeclStub s, @Nonnull StubOutputStream ds) throws IOException { }
            @Nonnull @Override public RsLetDeclStub deserialize(@Nonnull StubInputStream ds, StubElement p) throws IOException {
                return new RsLetDeclStub(p, this);
            }
            @Nonnull @Override public RsLetDeclStub createStub(@Nonnull RsLetDecl psi, @Nullable StubElement p) {
                return new RsLetDeclStub(p, this);
            }
            @Nonnull @Override public RsLetDecl createPsi(@Nonnull RsLetDeclStub s) { return new RsLetDeclImpl(s, this); }
        };
}
