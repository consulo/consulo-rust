/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */
package org.rust.lang.core.stubs;

import com.intellij.lang.ASTNode;
import com.intellij.psi.stubs.*;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.rust.lang.core.psi.RsTraitType;
import org.rust.lang.core.psi.impl.RsTraitTypeImpl;
import java.io.IOException;
import org.rust.lang.core.psi.ext.RsTraitTypeExtUtil;

public class RsTraitTypeStub extends StubBase<RsTraitType> {
    private final boolean isImpl;

    public RsTraitTypeStub(@Nullable StubElement<?> parent, @NotNull IStubElementType<?, ?> elementType, boolean isImpl) {
        super(parent, elementType); this.isImpl = isImpl;
    }
    public boolean isImpl() { return isImpl; }

    public static final RsStubElementType<RsTraitTypeStub, RsTraitType> Type =
        new RsStubElementType<RsTraitTypeStub, RsTraitType>("TRAIT_TYPE") {
            @Override public boolean shouldCreateStub(@NotNull ASTNode node) { return createStubIfParentIsStub(node); }
            @NotNull @Override public RsTraitTypeStub deserialize(@NotNull StubInputStream ds, StubElement p) throws IOException {
                return new RsTraitTypeStub(p, this, ds.readBoolean());
            }
            @Override public void serialize(@NotNull RsTraitTypeStub s, @NotNull StubOutputStream ds) throws IOException {
                ds.writeBoolean(s.isImpl);
            }
            @NotNull @Override public RsTraitType createPsi(@NotNull RsTraitTypeStub s) { return new RsTraitTypeImpl(s, this); }
            @NotNull @Override public RsTraitTypeStub createStub(@NotNull RsTraitType psi, @Nullable StubElement<?> p) {
                return new RsTraitTypeStub(p, this, RsTraitTypeExtUtil.isImpl(psi));
            }
        };
}
