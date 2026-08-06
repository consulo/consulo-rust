/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */
package org.rust.lang.core.stubs;

import consulo.language.psi.stub.*;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import org.rust.lang.core.psi.RsAlias;
import org.rust.lang.core.psi.impl.RsAliasImpl;
import java.io.IOException;

public class RsAliasStub extends StubBase<RsAlias> implements RsNamedStub {
    @Nullable private final String name;

    public RsAliasStub(@Nullable StubElement parent, @Nonnull IStubElementType elementType, @Nullable String name) {
        super(parent, elementType); this.name = name;
    }
    @Nullable @Override public String getName() { return name; }

    public static final RsStubElementType<RsAliasStub, RsAlias> Type =
        new RsStubElementType<RsAliasStub, RsAlias>("ALIAS") {
            @Nonnull @Override public RsAliasStub deserialize(@Nonnull StubInputStream ds, StubElement p) throws IOException {
                return new RsAliasStub(p, this, StubImplementationsKt.readNameAsString(ds));
            }
            @Override public void serialize(@Nonnull RsAliasStub s, @Nonnull StubOutputStream ds) throws IOException { ds.writeName(s.name); }
            @Nonnull @Override public RsAlias createPsi(@Nonnull RsAliasStub s) { return new RsAliasImpl(s, this); }
            @Nonnull @Override public RsAliasStub createStub(@Nonnull RsAlias psi, @Nullable StubElement p) {
                return new RsAliasStub(p, this, psi.getName());
            }
        };
}
