/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */
package org.rust.lang.core.stubs;

import com.intellij.lang.ASTNode;
import com.intellij.psi.stubs.*;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.rust.lang.core.psi.RsVis;
import org.rust.lang.core.psi.ext.RsVisStubKind;
import org.rust.lang.core.psi.impl.RsVisImpl;

import java.io.IOException;

public class RsVisStub extends StubBase<RsVis> {
    @NotNull
    private final RsVisStubKind kind;

    public RsVisStub(@Nullable StubElement<?> parent, @NotNull IStubElementType<?, ?> elementType, @NotNull RsVisStubKind kind) {
        super(parent, elementType);
        this.kind = kind;
    }

    @NotNull
    public RsVisStubKind getKind() {
        return kind;
    }

    @Nullable
    public RsPathStub getVisRestrictionPath() {
        for (StubElement<?> child : getChildrenStubs()) {
            if (child instanceof RsPlaceholderStub) {
                // VIS_RESTRICTION stub
                for (StubElement<?> grandChild : child.getChildrenStubs()) {
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
            public boolean shouldCreateStub(@NotNull ASTNode node) {
                return createStubIfParentIsStub(node);
            }

            @Override
            public void serialize(@NotNull RsVisStub stub, @NotNull StubOutputStream ds) throws IOException {
                ds.writeByte(stub.kind.ordinal());
            }

            @NotNull
            @Override
            public RsVisStub deserialize(@NotNull StubInputStream ds, StubElement parentStub) throws IOException {
                RsVisStubKind kind = RsVisStubKind.values()[ds.readUnsignedByte()];
                return new RsVisStub(parentStub, this, kind);
            }

            @NotNull
            @Override
            public RsVisStub createStub(@NotNull RsVis psi, @Nullable StubElement<?> parentStub) {
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

            @NotNull
            @Override
            public RsVis createPsi(@NotNull RsVisStub stub) {
                return new RsVisImpl(stub, this);
            }
        };
}
