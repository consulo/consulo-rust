/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */
package org.rust.lang.core.stubs;

import com.intellij.lang.ASTNode;
import com.intellij.psi.stubs.*;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.rust.lang.core.psi.RsValueParameter;
import org.rust.lang.core.psi.impl.RsValueParameterImpl;
import java.io.IOException;
import org.rust.lang.core.psi.ext.RsValueParameterUtil;

public class RsValueParameterStub extends RsAttributeOwnerStubBase<RsValueParameter> {
    @Nullable private final String patText;
    private final int flags;

    public RsValueParameterStub(@Nullable StubElement<?> parent, @NotNull IStubElementType<?, ?> elementType,
                                 @Nullable String patText, int flags) {
        super(parent, elementType); this.patText = patText; this.flags = flags;
    }
    @Nullable public String getPatText() { return patText; }
    @Override protected int getFlags() { return flags; }

    public static final RsStubElementType<RsValueParameterStub, RsValueParameter> Type =
        new RsStubElementType<RsValueParameterStub, RsValueParameter>("VALUE_PARAMETER") {
            @Override public boolean shouldCreateStub(@NotNull ASTNode node) { return createStubIfParentIsStub(node); }
            @NotNull @Override public RsValueParameterStub deserialize(@NotNull StubInputStream ds, StubElement p) throws IOException {
                return new RsValueParameterStub(p, this, StubImplementationsKt.readNameAsString(ds), ds.readUnsignedByte());
            }
            @Override public void serialize(@NotNull RsValueParameterStub s, @NotNull StubOutputStream ds) throws IOException {
                ds.writeName(s.patText); ds.writeByte(s.flags);
            }
            @NotNull @Override public RsValueParameter createPsi(@NotNull RsValueParameterStub s) { return new RsValueParameterImpl(s, this); }
            @NotNull @Override public RsValueParameterStub createStub(@NotNull RsValueParameter psi, @Nullable StubElement<?> p) {
                return new RsValueParameterStub(p, this, RsValueParameterUtil.getPatText(psi), RsAttributeOwnerStub.extractFlags(psi));
            }
        };
}
