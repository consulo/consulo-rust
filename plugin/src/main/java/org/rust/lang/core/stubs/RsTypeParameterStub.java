/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */
package org.rust.lang.core.stubs;

import consulo.language.psi.stub.*;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import org.rust.lang.core.psi.RsTypeParameter;
import org.rust.lang.core.psi.impl.RsTypeParameterImpl;
import java.io.IOException;

public class RsTypeParameterStub extends RsAttributeOwnerStubBase<RsTypeParameter> implements RsNamedStub {
    @Nullable private final String name; private final int flags;
    public RsTypeParameterStub(@Nullable StubElement p, @Nonnull IStubElementType et, @Nullable String name, int flags) {
        super(p, et); this.name = name; this.flags = flags;
    }
    @Nullable @Override public String getName() { return name; }
    @Override protected int getFlags() { return flags; }

    public static final RsStubElementType<RsTypeParameterStub, RsTypeParameter> Type =
        new RsStubElementType<RsTypeParameterStub, RsTypeParameter>("TYPE_PARAMETER") {
            @Nonnull @Override public RsTypeParameterStub deserialize(@Nonnull StubInputStream ds, StubElement p) throws IOException {
                return new RsTypeParameterStub(p, this, StubImplementationsKt.readNameAsString(ds), ds.readUnsignedByte());
            }
            @Override public void serialize(@Nonnull RsTypeParameterStub s, @Nonnull StubOutputStream ds) throws IOException { ds.writeName(s.name); ds.writeByte(s.flags); }
            @Nonnull @Override public RsTypeParameter createPsi(@Nonnull RsTypeParameterStub s) { return new RsTypeParameterImpl(s, this); }
            @Nonnull @Override public RsTypeParameterStub createStub(@Nonnull RsTypeParameter psi, @Nullable StubElement p) {
                return new RsTypeParameterStub(p, this, psi.getName(), RsAttributeOwnerStub.extractFlags(psi));
            }
        };
}
