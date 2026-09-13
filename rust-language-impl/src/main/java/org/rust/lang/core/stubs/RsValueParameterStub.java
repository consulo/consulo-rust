/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */
package org.rust.lang.core.stubs;

import consulo.language.ast.ASTNode;
import consulo.language.psi.stub.*;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import org.rust.lang.core.psi.RsValueParameter;
import java.io.IOException;
import org.rust.lang.core.psi.ext.impl.RsValueParameterUtil;
import org.rust.lang.core.psi.impl.RsValueParameterImpl;

public class RsValueParameterStub extends RsAttributeOwnerStubBase<RsValueParameter> {
    @Nullable public final String patText;
    public final int flags;

    public RsValueParameterStub(@Nullable StubElement parent, @Nonnull IStubElementType elementType,
                                 @Nullable String patText, int flags) {
        super(parent, elementType); this.patText = patText; this.flags = flags;
    }
    @Nullable public String getPatText() { return patText; }
    @Override protected int getFlags() { return flags; }


    public static final RsStubElementType<RsValueParameterStub, RsValueParameter> Type =
        new RsStubElementType<RsValueParameterStub, RsValueParameter>("VALUE_PARAMETER") {
            @Override public boolean shouldCreateStub(@Nonnull ASTNode node) { return createStubIfParentIsStub(node); }
            @Nonnull @Override public RsValueParameterStub deserialize(@Nonnull StubInputStream ds, StubElement p) throws IOException {
                return new RsValueParameterStub(p, this, StubImplementationsKt.readNameAsString(ds), ds.readUnsignedByte());
            }
            @Override public void serialize(@Nonnull RsValueParameterStub s, @Nonnull StubOutputStream ds) throws IOException {
                ds.writeName(s.patText); ds.writeByte(s.flags);
            }
            @Nonnull @Override public RsValueParameter createPsi(@Nonnull RsValueParameterStub s) { return new RsValueParameterImpl(s, this); }
            @Nonnull @Override public RsValueParameterStub createStub(@Nonnull RsValueParameter psi, @Nullable StubElement p) {
                return new RsValueParameterStub(p, this, RsValueParameterUtil.getPatText(psi), RsAttributeOwnerStub.extractFlags(psi));
            }
        };
}
