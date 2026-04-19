/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */
package org.rust.lang.core.stubs;

import com.intellij.lang.ASTNode;
import com.intellij.psi.stubs.*;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.rust.lang.core.psi.RsRefLikeType;
import org.rust.lang.core.psi.impl.RsRefLikeTypeImpl;
import java.io.IOException;
import org.rust.lang.core.psi.ext.RsRefLikeTypeUtil;

public class RsRefLikeTypeStub extends StubBase<RsRefLikeType> {
    private final boolean isMut;
    private final boolean isRef;
    private final boolean isPointer;

    public RsRefLikeTypeStub(@Nullable StubElement<?> parent, @NotNull IStubElementType<?, ?> elementType,
                              boolean isMut, boolean isRef, boolean isPointer) {
        super(parent, elementType);
        this.isMut = isMut; this.isRef = isRef; this.isPointer = isPointer;
    }
    public boolean isMut() { return isMut; }
    public boolean isRef() { return isRef; }
    public boolean isPointer() { return isPointer; }

    public static final RsStubElementType<RsRefLikeTypeStub, RsRefLikeType> Type =
        new RsStubElementType<RsRefLikeTypeStub, RsRefLikeType>("REF_LIKE_TYPE") {
            @Override public boolean shouldCreateStub(@NotNull ASTNode node) { return createStubIfParentIsStub(node); }
            @NotNull @Override public RsRefLikeTypeStub deserialize(@NotNull StubInputStream ds, StubElement p) throws IOException {
                return new RsRefLikeTypeStub(p, this, ds.readBoolean(), ds.readBoolean(), ds.readBoolean());
            }
            @Override public void serialize(@NotNull RsRefLikeTypeStub s, @NotNull StubOutputStream ds) throws IOException {
                ds.writeBoolean(s.isMut); ds.writeBoolean(s.isRef); ds.writeBoolean(s.isPointer);
            }
            @NotNull @Override public RsRefLikeType createPsi(@NotNull RsRefLikeTypeStub s) { return new RsRefLikeTypeImpl(s, this); }
            @NotNull @Override public RsRefLikeTypeStub createStub(@NotNull RsRefLikeType psi, @Nullable StubElement<?> p) {
                return new RsRefLikeTypeStub(p, this, RsRefLikeTypeUtil.getMutability(psi).isMut(), RsRefLikeTypeUtil.isRef(psi), RsRefLikeTypeUtil.isPointer(psi));
            }
        };
}
