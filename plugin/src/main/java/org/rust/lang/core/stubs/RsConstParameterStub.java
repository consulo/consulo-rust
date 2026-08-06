/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */
package org.rust.lang.core.stubs;

import consulo.language.psi.stub.*;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import org.rust.lang.core.psi.RsConstParameter;
import org.rust.lang.core.psi.impl.RsConstParameterImpl;
import java.io.IOException;

public class RsConstParameterStub extends RsAttributeOwnerStubBase<RsConstParameter> implements RsNamedStub {
    @Nullable private final String name;
    private final int flags;

    public RsConstParameterStub(@Nullable StubElement parent, @Nonnull IStubElementType elementType,
                                 @Nullable String name, int flags) {
        super(parent, elementType); this.name = name; this.flags = flags;
    }
    @Nullable @Override public String getName() { return name; }
    @Override protected int getFlags() { return flags; }

    public static final RsStubElementType<RsConstParameterStub, RsConstParameter> Type =
        new RsStubElementType<RsConstParameterStub, RsConstParameter>("CONST_PARAMETER") {
            @Nonnull @Override public RsConstParameterStub deserialize(@Nonnull StubInputStream ds, StubElement p) throws IOException {
                return new RsConstParameterStub(p, this, StubImplementationsKt.readNameAsString(ds), ds.readUnsignedByte());
            }
            @Override public void serialize(@Nonnull RsConstParameterStub s, @Nonnull StubOutputStream ds) throws IOException {
                ds.writeName(s.name); ds.writeByte(s.flags);
            }
            @Nonnull @Override public RsConstParameter createPsi(@Nonnull RsConstParameterStub s) { return new RsConstParameterImpl(s, this); }
            @Nonnull @Override public RsConstParameterStub createStub(@Nonnull RsConstParameter psi, @Nullable StubElement p) {
                return new RsConstParameterStub(p, this, psi.getName(), RsAttributeOwnerStub.extractFlags(psi));
            }
        };
}
