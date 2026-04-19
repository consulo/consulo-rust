/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */
package org.rust.lang.core.stubs;

import com.intellij.lang.ASTNode;
import com.intellij.psi.stubs.*;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.rust.lang.core.psi.RsPath;
import org.rust.lang.core.psi.ext.PathKind;
import org.rust.lang.core.psi.impl.RsPathImpl;
import org.rust.lang.core.stubs.common.RsPathPsiOrStub;
import org.rust.stdext.IoUtil;
import java.io.IOException;

public class RsPathStub extends StubBase<RsPath> implements RsPathPsiOrStub {
    @Nullable private final String referenceName;
    private final boolean hasColonColon;
    @NotNull private final PathKind kind;
    private final int startOffset;

    public RsPathStub(@Nullable StubElement<?> parent, @NotNull IStubElementType<?, ?> elementType,
                       @Nullable String referenceName, boolean hasColonColon, @NotNull PathKind kind, int startOffset) {
        super(parent, elementType);
        this.referenceName = referenceName; this.hasColonColon = hasColonColon; this.kind = kind; this.startOffset = startOffset;
    }

    @Nullable @Override public String getReferenceName() { return referenceName; }
    @Override public boolean getHasColonColon() { return hasColonColon; }
    @NotNull @Override public PathKind getKind() { return kind; }
    public int getStartOffset() { return startOffset; }
    @Nullable @Override public RsPathStub getPath() { return (RsPathStub) findChildStubByType(Type); }

    public static final RsStubElementType<RsPathStub, RsPath> Type =
        new RsStubElementType<RsPathStub, RsPath>("PATH") {
            @Override public boolean shouldCreateStub(@NotNull ASTNode node) { return createStubIfParentIsStub(node); }
            @NotNull @Override public RsPathStub deserialize(@NotNull StubInputStream ds, StubElement p) throws IOException {
                String name = ds.readName() != null ? ds.readName().getString() : null;
                return new RsPathStub(p, this, name, ds.readBoolean(),
                    IoUtil.readEnum(ds, PathKind.class), ds.readVarInt());
            }
            @Override public void serialize(@NotNull RsPathStub s, @NotNull StubOutputStream ds) throws IOException {
                ds.writeName(s.referenceName); ds.writeBoolean(s.hasColonColon);
                IoUtil.writeEnum(ds, s.kind); ds.writeVarInt(s.startOffset);
            }
            @NotNull @Override public RsPath createPsi(@NotNull RsPathStub s) { return new RsPathImpl(s, this); }
            @NotNull @Override public RsPathStub createStub(@NotNull RsPath psi, @Nullable StubElement<?> p) {
                return new RsPathStub(p, this, psi.getReferenceName(), psi.getHasColonColon(), psi.getKind(), psi.getTextRange().getStartOffset());
            }
        };
}
