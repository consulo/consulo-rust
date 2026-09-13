/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */
package org.rust.lang.core.stubs;

import consulo.language.ast.ASTNode;
import consulo.language.psi.stub.*;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import org.rust.lang.core.psi.RsPathType;
import java.io.IOException;
import org.rust.lang.core.psi.impl.RsPathTypeImpl;

public class RsPathTypeStub extends StubBase<RsPathType> {

    private RsPathTypeStub(@Nullable StubElement parent, @Nonnull IStubElementType elementType) {
        super(parent, elementType);
    }


    public static final RsStubElementType<RsPathTypeStub, RsPathType> Type =
        new RsStubElementType<RsPathTypeStub, RsPathType>("PATH_TYPE") {
            @Override public boolean shouldCreateStub(@Nonnull ASTNode node) { return createStubIfParentIsStub(node); }
            @Nonnull @Override public RsPathTypeStub deserialize(@Nonnull StubInputStream ds, StubElement p) throws IOException {
                return new RsPathTypeStub(p, this);
            }
            @Override public void serialize(@Nonnull RsPathTypeStub s, @Nonnull StubOutputStream ds) throws IOException { }
            @Nonnull @Override public RsPathType createPsi(@Nonnull RsPathTypeStub s) { return new RsPathTypeImpl(s, this); }
            @Nonnull @Override public RsPathTypeStub createStub(@Nonnull RsPathType psi, @Nullable StubElement p) {
                return new RsPathTypeStub(p, this);
            }
        };
}
