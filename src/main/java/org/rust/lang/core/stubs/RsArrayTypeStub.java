/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */
package org.rust.lang.core.stubs;

import com.intellij.lang.ASTNode;
import com.intellij.psi.stubs.*;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.rust.lang.core.psi.RsArrayType;
import org.rust.lang.core.psi.impl.RsArrayTypeImpl;
import java.io.IOException;
import org.rust.lang.core.psi.ext.RsArrayTypeUtil;

public class RsArrayTypeStub extends StubBase<RsArrayType> {
    private final boolean isSlice;

    public RsArrayTypeStub(@Nullable StubElement<?> parent, @NotNull IStubElementType<?, ?> elementType, boolean isSlice) {
        super(parent, elementType); this.isSlice = isSlice;
    }
    public boolean isSlice() { return isSlice; }

    public static final RsStubElementType<RsArrayTypeStub, RsArrayType> Type =
        new RsStubElementType<RsArrayTypeStub, RsArrayType>("ARRAY_TYPE") {
            @Override public boolean shouldCreateStub(@NotNull ASTNode node) { return createStubIfParentIsStub(node); }
            @NotNull @Override public RsArrayTypeStub deserialize(@NotNull StubInputStream ds, StubElement p) throws IOException {
                return new RsArrayTypeStub(p, this, ds.readBoolean());
            }
            @Override public void serialize(@NotNull RsArrayTypeStub s, @NotNull StubOutputStream ds) throws IOException {
                ds.writeBoolean(s.isSlice);
            }
            @NotNull @Override public RsArrayType createPsi(@NotNull RsArrayTypeStub s) { return new RsArrayTypeImpl(s, this); }
            @NotNull @Override public RsArrayTypeStub createStub(@NotNull RsArrayType psi, @Nullable StubElement<?> p) {
                return new RsArrayTypeStub(p, this, RsArrayTypeUtil.isSlice(psi));
            }
        };
}
