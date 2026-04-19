/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */
package org.rust.lang.core.stubs;

import com.intellij.lang.ASTNode;
import com.intellij.psi.stubs.*;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.rust.lang.core.psi.RsLetDecl;
import org.rust.lang.core.psi.impl.RsLetDeclImpl;
import java.io.IOException;

public class RsLetDeclStub extends StubBase<RsLetDecl> {

    public RsLetDeclStub(@Nullable StubElement<?> parent, @NotNull IStubElementType<?, ?> elementType) {
        super(parent, elementType);
    }

    public static final RsStubElementType<RsLetDeclStub, RsLetDecl> Type =
        new RsStubElementType<RsLetDeclStub, RsLetDecl>("LET_DECL") {
            @Override public boolean shouldCreateStub(@NotNull ASTNode node) { return StubImplementationsKt.shouldCreateStmtStub(node); }
            @Override public void serialize(@NotNull RsLetDeclStub s, @NotNull StubOutputStream ds) throws IOException { }
            @NotNull @Override public RsLetDeclStub deserialize(@NotNull StubInputStream ds, StubElement p) throws IOException {
                return new RsLetDeclStub(p, this);
            }
            @NotNull @Override public RsLetDeclStub createStub(@NotNull RsLetDecl psi, @Nullable StubElement<?> p) {
                return new RsLetDeclStub(p, this);
            }
            @NotNull @Override public RsLetDecl createPsi(@NotNull RsLetDeclStub s) { return new RsLetDeclImpl(s, this); }
        };
}
