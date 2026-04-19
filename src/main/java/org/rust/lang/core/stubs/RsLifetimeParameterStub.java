/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */
package org.rust.lang.core.stubs;

import com.intellij.lang.ASTNode;
import com.intellij.psi.stubs.*;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.rust.lang.core.psi.RsLifetimeParameter;
import org.rust.lang.core.psi.impl.RsLifetimeParameterImpl;
import java.io.IOException;

public class RsLifetimeParameterStub extends StubBase<RsLifetimeParameter> implements RsNamedStub {
    @Nullable private final String name;

    public RsLifetimeParameterStub(@Nullable StubElement<?> parent, @NotNull IStubElementType<?, ?> elementType, @Nullable String name) {
        super(parent, elementType); this.name = name;
    }
    @Nullable @Override public String getName() { return name; }

    public static final RsStubElementType<RsLifetimeParameterStub, RsLifetimeParameter> Type =
        new RsStubElementType<RsLifetimeParameterStub, RsLifetimeParameter>("LIFETIME_PARAMETER") {
            @Override public boolean shouldCreateStub(@NotNull ASTNode node) { return createStubIfParentIsStub(node); }
            @NotNull @Override public RsLifetimeParameterStub deserialize(@NotNull StubInputStream ds, StubElement p) throws IOException {
                return new RsLifetimeParameterStub(p, this, StubImplementationsKt.readNameAsString(ds));
            }
            @Override public void serialize(@NotNull RsLifetimeParameterStub s, @NotNull StubOutputStream ds) throws IOException {
                ds.writeName(s.name);
            }
            @NotNull @Override public RsLifetimeParameter createPsi(@NotNull RsLifetimeParameterStub s) { return new RsLifetimeParameterImpl(s, this); }
            @NotNull @Override public RsLifetimeParameterStub createStub(@NotNull RsLifetimeParameter psi, @Nullable StubElement<?> p) {
                return new RsLifetimeParameterStub(p, this, psi.getName());
            }
        };
}
