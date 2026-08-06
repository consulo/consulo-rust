/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */
package org.rust.lang.core.stubs;

import consulo.language.ast.ASTNode;
import consulo.language.psi.stub.*;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import org.rust.lang.core.psi.RsLifetime;
import org.rust.lang.core.psi.impl.RsLifetimeImpl;
import java.io.IOException;

public class RsLifetimeStub extends StubBase<RsLifetime> implements RsNamedStub {
    @Nullable private final String name;

    public RsLifetimeStub(@Nullable StubElement parent, @Nonnull IStubElementType elementType, @Nullable String name) {
        super(parent, elementType); this.name = name;
    }
    @Nullable @Override public String getName() { return name; }

    public static final RsStubElementType<RsLifetimeStub, RsLifetime> Type =
        new RsStubElementType<RsLifetimeStub, RsLifetime>("LIFETIME") {
            @Override public boolean shouldCreateStub(@Nonnull ASTNode node) { return createStubIfParentIsStub(node); }
            @Nonnull @Override public RsLifetimeStub deserialize(@Nonnull StubInputStream ds, StubElement p) throws IOException {
                return new RsLifetimeStub(p, this, StubImplementationsKt.readNameAsString(ds));
            }
            @Override public void serialize(@Nonnull RsLifetimeStub s, @Nonnull StubOutputStream ds) throws IOException {
                ds.writeName(s.name);
            }
            @Nonnull @Override public RsLifetime createPsi(@Nonnull RsLifetimeStub s) { return new RsLifetimeImpl(s, this); }
            @Nonnull @Override public RsLifetimeStub createStub(@Nonnull RsLifetime psi, @Nullable StubElement p) {
                return new RsLifetimeStub(p, this, psi.getReferenceName());
            }
        };
}
