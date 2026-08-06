/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */
package org.rust.lang.core.stubs;

import consulo.language.ast.ASTNode;
import consulo.language.psi.stub.*;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import org.rust.lang.core.psi.RsVis;
import org.rust.lang.core.psi.ext.RsVisStubKind;
import org.rust.lang.core.psi.impl.RsVisImpl;

import java.io.IOException;

public class RsVisStub extends StubBase<RsVis> {
    @Nonnull
    private final RsVisStubKind kind;

    public RsVisStub(@Nullable StubElement parent, @Nonnull IStubElementType elementType, @Nonnull RsVisStubKind kind) {
        super(parent, elementType);
        this.kind = kind;
    }

    @Nonnull
    public RsVisStubKind getKind() {
        return kind;
    }

    @Nullable
    public RsPathStub getVisRestrictionPath() {
        for (StubElement child : getChildrenStubs()) {
            if (child instanceof RsPlaceholderStub) {
                // VIS_RESTRICTION stub
                for (Object grandChild : child.getChildrenStubs()) {
                    if (grandChild instanceof RsPathStub) {
                        return (RsPathStub) grandChild;
                    }
                }
            }
        }
        return null;
    }

    public static final RsStubElementType<RsVisStub, RsVis> Type =
        new RsStubElementType<RsVisStub, RsVis>("VIS") {
            @Override
            public boolean shouldCreateStub(@Nonnull ASTNode node) {
                return createStubIfParentIsStub(node);
            }

            @Override
            public void serialize(@Nonnull RsVisStub stub, @Nonnull StubOutputStream ds) throws IOException {
                ds.writeByte(stub.kind.ordinal());
            }

            @Nonnull
            @Override
            public RsVisStub deserialize(@Nonnull StubInputStream ds, StubElement parentStub) throws IOException {
                RsVisStubKind kind = RsVisStubKind.values()[ds.readUnsignedByte()];
                return new RsVisStub(parentStub, this, kind);
            }

            @Nonnull
            @Override
            public RsVisStub createStub(@Nonnull RsVis psi, @Nullable StubElement parentStub) {
                RsVisStubKind kind;
                if (psi.getCrate() != null) {
                    kind = RsVisStubKind.CRATE;
                } else if (psi.getVisRestriction() != null) {
                    kind = RsVisStubKind.RESTRICTED;
                } else {
                    kind = RsVisStubKind.PUB;
                }
                return new RsVisStub(parentStub, this, kind);
            }

            @Nonnull
            @Override
            public RsVis createPsi(@Nonnull RsVisStub stub) {
                return new RsVisImpl(stub, this);
            }
        };
}
