/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */
package org.rust.lang.core.stubs;

import consulo.language.ast.ASTNode;
import consulo.language.psi.stub.*;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import org.rust.lang.core.psi.RsRefLikeType;
import java.io.IOException;
import org.rust.lang.core.psi.ext.impl.RsRefLikeTypeUtil;
import org.rust.lang.core.psi.impl.RsRefLikeTypeImpl;

public class RsRefLikeTypeStub extends StubBase<RsRefLikeType> {
    public final boolean isMut;
    public final boolean isRef;
    public final boolean isPointer;

    public RsRefLikeTypeStub(@Nullable StubElement parent, @Nonnull IStubElementType elementType,
                              boolean isMut, boolean isRef, boolean isPointer) {
        super(parent, elementType);
        this.isMut = isMut; this.isRef = isRef; this.isPointer = isPointer;
    }
    public boolean isMut() { return isMut; }
    public boolean isRef() { return isRef; }
    public boolean isPointer() { return isPointer; }


    public static final RsStubElementType<RsRefLikeTypeStub, RsRefLikeType> Type =
        new RsStubElementType<RsRefLikeTypeStub, RsRefLikeType>("REF_LIKE_TYPE") {
            @Override public boolean shouldCreateStub(@Nonnull ASTNode node) { return createStubIfParentIsStub(node); }
            @Nonnull @Override public RsRefLikeTypeStub deserialize(@Nonnull StubInputStream ds, StubElement p) throws IOException {
                return new RsRefLikeTypeStub(p, this, ds.readBoolean(), ds.readBoolean(), ds.readBoolean());
            }
            @Override public void serialize(@Nonnull RsRefLikeTypeStub s, @Nonnull StubOutputStream ds) throws IOException {
                ds.writeBoolean(s.isMut); ds.writeBoolean(s.isRef); ds.writeBoolean(s.isPointer);
            }
            @Nonnull @Override public RsRefLikeType createPsi(@Nonnull RsRefLikeTypeStub s) { return new RsRefLikeTypeImpl(s, this); }
            @Nonnull @Override public RsRefLikeTypeStub createStub(@Nonnull RsRefLikeType psi, @Nullable StubElement p) {
                return new RsRefLikeTypeStub(p, this, RsRefLikeTypeUtil.getMutability(psi).isMut(), RsRefLikeTypeUtil.isRef(psi), RsRefLikeTypeUtil.isPointer(psi));
            }
        };
}
