/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */
package org.rust.lang.core.stubs;

import consulo.language.ast.ASTNode;
import consulo.language.psi.stub.*;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import org.rust.lang.core.psi.RsArrayType;
import org.rust.lang.core.psi.impl.RsArrayTypeImpl;
import java.io.IOException;
import org.rust.lang.core.psi.ext.RsArrayTypeUtil;

public class RsArrayTypeStub extends StubBase<RsArrayType> {
    private final boolean isSlice;

    public RsArrayTypeStub(@Nullable StubElement parent, @Nonnull IStubElementType elementType, boolean isSlice) {
        super(parent, elementType); this.isSlice = isSlice;
    }
    public boolean isSlice() { return isSlice; }

    public static final RsStubElementType<RsArrayTypeStub, RsArrayType> Type =
        new RsStubElementType<RsArrayTypeStub, RsArrayType>("ARRAY_TYPE") {
            @Override public boolean shouldCreateStub(@Nonnull ASTNode node) { return createStubIfParentIsStub(node); }
            @Nonnull @Override public RsArrayTypeStub deserialize(@Nonnull StubInputStream ds, StubElement p) throws IOException {
                return new RsArrayTypeStub(p, this, ds.readBoolean());
            }
            @Override public void serialize(@Nonnull RsArrayTypeStub s, @Nonnull StubOutputStream ds) throws IOException {
                ds.writeBoolean(s.isSlice);
            }
            @Nonnull @Override public RsArrayType createPsi(@Nonnull RsArrayTypeStub s) { return new RsArrayTypeImpl(s, this); }
            @Nonnull @Override public RsArrayTypeStub createStub(@Nonnull RsArrayType psi, @Nullable StubElement p) {
                return new RsArrayTypeStub(p, this, RsArrayTypeUtil.isSlice(psi));
            }
        };
}
