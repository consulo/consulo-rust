/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */
package org.rust.lang.core.stubs;

import consulo.language.ast.ASTNode;
import consulo.language.psi.stub.*;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import org.rust.lang.core.psi.RsLifetimeParameter;
import java.io.IOException;
import org.rust.lang.core.psi.impl.RsLifetimeParameterImpl;

public class RsLifetimeParameterStub extends StubBase<RsLifetimeParameter> implements RsNamedStub {
    @Nullable public final String name;

    public RsLifetimeParameterStub(@Nullable StubElement parent, @Nonnull IStubElementType elementType, @Nullable String name) {
        super(parent, elementType); this.name = name;
    }
    @Nullable @Override public String getName() { return name; }


    public static final RsStubElementType<RsLifetimeParameterStub, RsLifetimeParameter> Type =
        new RsStubElementType<RsLifetimeParameterStub, RsLifetimeParameter>("LIFETIME_PARAMETER") {
            @Override public boolean shouldCreateStub(@Nonnull ASTNode node) { return createStubIfParentIsStub(node); }
            @Nonnull @Override public RsLifetimeParameterStub deserialize(@Nonnull StubInputStream ds, StubElement p) throws IOException {
                return new RsLifetimeParameterStub(p, this, StubImplementationsKt.readNameAsString(ds));
            }
            @Override public void serialize(@Nonnull RsLifetimeParameterStub s, @Nonnull StubOutputStream ds) throws IOException {
                ds.writeName(s.name);
            }
            @Nonnull @Override public RsLifetimeParameter createPsi(@Nonnull RsLifetimeParameterStub s) { return new RsLifetimeParameterImpl(s, this); }
            @Nonnull @Override public RsLifetimeParameterStub createStub(@Nonnull RsLifetimeParameter psi, @Nullable StubElement p) {
                return new RsLifetimeParameterStub(p, this, psi.getName());
            }
        };
}
