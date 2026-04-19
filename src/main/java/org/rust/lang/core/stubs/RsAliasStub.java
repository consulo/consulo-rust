/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */
package org.rust.lang.core.stubs;

import com.intellij.psi.stubs.*;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.rust.lang.core.psi.RsAlias;
import org.rust.lang.core.psi.impl.RsAliasImpl;
import java.io.IOException;

public class RsAliasStub extends StubBase<RsAlias> implements RsNamedStub {
    @Nullable private final String name;

    public RsAliasStub(@Nullable StubElement<?> parent, @NotNull IStubElementType<?, ?> elementType, @Nullable String name) {
        super(parent, elementType); this.name = name;
    }
    @Nullable @Override public String getName() { return name; }

    public static final RsStubElementType<RsAliasStub, RsAlias> Type =
        new RsStubElementType<RsAliasStub, RsAlias>("ALIAS") {
            @NotNull @Override public RsAliasStub deserialize(@NotNull StubInputStream ds, StubElement p) throws IOException {
                return new RsAliasStub(p, this, StubImplementationsKt.readNameAsString(ds));
            }
            @Override public void serialize(@NotNull RsAliasStub s, @NotNull StubOutputStream ds) throws IOException { ds.writeName(s.name); }
            @NotNull @Override public RsAlias createPsi(@NotNull RsAliasStub s) { return new RsAliasImpl(s, this); }
            @NotNull @Override public RsAliasStub createStub(@NotNull RsAlias psi, @Nullable StubElement<?> p) {
                return new RsAliasStub(p, this, psi.getName());
            }
        };
}
