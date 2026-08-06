/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */
package org.rust.lang.core.stubs;

import consulo.language.ast.ASTNode;
import consulo.language.psi.stub.*;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import org.rust.lang.core.psi.RsPolybound;
import org.rust.lang.core.psi.impl.RsPolyboundImpl;

import java.io.IOException;

public class RsPolyboundStub extends StubBase<RsPolybound> {
    private final boolean hasQ;
    private final boolean hasConst;

    public RsPolyboundStub(@Nullable StubElement parent, @Nonnull IStubElementType elementType, boolean hasQ, boolean hasConst) {
        super(parent, elementType);
        this.hasQ = hasQ;
        this.hasConst = hasConst;
    }

    public boolean getHasQ() {
        return hasQ;
    }

    public boolean getHasConst() {
        return hasConst;
    }

    public static final RsStubElementType<RsPolyboundStub, RsPolybound> Type =
        new RsStubElementType<RsPolyboundStub, RsPolybound>("POLYBOUND") {
            @Override
            public boolean shouldCreateStub(@Nonnull ASTNode node) {
                return createStubIfParentIsStub(node);
            }

            @Override
            public void serialize(@Nonnull RsPolyboundStub stub, @Nonnull StubOutputStream ds) throws IOException {
                int flags = 0;
                if (stub.hasQ) flags |= 1;
                if (stub.hasConst) flags |= 2;
                ds.writeByte(flags);
            }

            @Nonnull
            @Override
            public RsPolyboundStub deserialize(@Nonnull StubInputStream ds, StubElement parentStub) throws IOException {
                int flags = ds.readUnsignedByte();
                boolean hasQ = (flags & 1) != 0;
                boolean hasConst = (flags & 2) != 0;
                return new RsPolyboundStub(parentStub, this, hasQ, hasConst);
            }

            @Nonnull
            @Override
            public RsPolyboundStub createStub(@Nonnull RsPolybound psi, @Nullable StubElement parentStub) {
                return new RsPolyboundStub(parentStub, this, psi.getQ() != null, psi.getTildeConst() != null);
            }

            @Nonnull
            @Override
            public RsPolybound createPsi(@Nonnull RsPolyboundStub stub) {
                return new RsPolyboundImpl(stub, this);
            }
        };
}
