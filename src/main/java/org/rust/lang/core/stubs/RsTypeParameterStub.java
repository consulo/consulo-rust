/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */
package org.rust.lang.core.stubs;

import com.intellij.psi.stubs.*; import org.jetbrains.annotations.NotNull; import org.jetbrains.annotations.Nullable;
import org.rust.lang.core.psi.RsTypeParameter; import org.rust.lang.core.psi.impl.RsTypeParameterImpl;
import java.io.IOException;

public class RsTypeParameterStub extends RsAttributeOwnerStubBase<RsTypeParameter> implements RsNamedStub {
    @Nullable private final String name; private final int flags;
    public RsTypeParameterStub(@Nullable StubElement<?> p, @NotNull IStubElementType<?, ?> et, @Nullable String name, int flags) {
        super(p, et); this.name = name; this.flags = flags;
    }
    @Nullable @Override public String getName() { return name; }
    @Override protected int getFlags() { return flags; }

    public static final RsStubElementType<RsTypeParameterStub, RsTypeParameter> Type =
        new RsStubElementType<RsTypeParameterStub, RsTypeParameter>("TYPE_PARAMETER") {
            @NotNull @Override public RsTypeParameterStub deserialize(@NotNull StubInputStream ds, StubElement p) throws IOException {
                return new RsTypeParameterStub(p, this, StubImplementationsKt.readNameAsString(ds), ds.readUnsignedByte());
            }
            @Override public void serialize(@NotNull RsTypeParameterStub s, @NotNull StubOutputStream ds) throws IOException { ds.writeName(s.name); ds.writeByte(s.flags); }
            @NotNull @Override public RsTypeParameter createPsi(@NotNull RsTypeParameterStub s) { return new RsTypeParameterImpl(s, this); }
            @NotNull @Override public RsTypeParameterStub createStub(@NotNull RsTypeParameter psi, @Nullable StubElement<?> p) {
                return new RsTypeParameterStub(p, this, psi.getName(), RsAttributeOwnerStub.extractFlags(psi));
            }
        };
}
