/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */
package org.rust.lang.core.stubs;

import com.intellij.lang.ASTNode;
import com.intellij.psi.stubs.*;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.rust.lang.core.psi.RsLifetime;
import org.rust.lang.core.psi.impl.RsLifetimeImpl;
import java.io.IOException;

public class RsLifetimeStub extends StubBase<RsLifetime> implements RsNamedStub {
    @Nullable private final String name;

    public RsLifetimeStub(@Nullable StubElement<?> parent, @NotNull IStubElementType<?, ?> elementType, @Nullable String name) {
        super(parent, elementType); this.name = name;
    }
    @Nullable @Override public String getName() { return name; }

    public static final RsStubElementType<RsLifetimeStub, RsLifetime> Type =
        new RsStubElementType<RsLifetimeStub, RsLifetime>("LIFETIME") {
            @Override public boolean shouldCreateStub(@NotNull ASTNode node) { return createStubIfParentIsStub(node); }
            @NotNull @Override public RsLifetimeStub deserialize(@NotNull StubInputStream ds, StubElement p) throws IOException {
                return new RsLifetimeStub(p, this, StubImplementationsKt.readNameAsString(ds));
            }
            @Override public void serialize(@NotNull RsLifetimeStub s, @NotNull StubOutputStream ds) throws IOException {
                ds.writeName(s.name);
            }
            @NotNull @Override public RsLifetime createPsi(@NotNull RsLifetimeStub s) { return new RsLifetimeImpl(s, this); }
            @NotNull @Override public RsLifetimeStub createStub(@NotNull RsLifetime psi, @Nullable StubElement<?> p) {
                return new RsLifetimeStub(p, this, psi.getReferenceName());
            }
        };
}
