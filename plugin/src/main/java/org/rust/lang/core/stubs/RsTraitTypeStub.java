/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */
package org.rust.lang.core.stubs;

import consulo.language.ast.ASTNode;
import consulo.language.psi.stub.*;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import org.rust.lang.core.psi.RsTraitType;
import org.rust.lang.core.psi.impl.RsTraitTypeImpl;
import java.io.IOException;
import org.rust.lang.core.psi.ext.RsTraitTypeExtUtil;

public class RsTraitTypeStub extends StubBase<RsTraitType> {
    private final boolean isImpl;

    public RsTraitTypeStub(@Nullable StubElement parent, @Nonnull IStubElementType elementType, boolean isImpl) {
        super(parent, elementType); this.isImpl = isImpl;
    }
    public boolean isImpl() { return isImpl; }

    public static final RsStubElementType<RsTraitTypeStub, RsTraitType> Type =
        new RsStubElementType<RsTraitTypeStub, RsTraitType>("TRAIT_TYPE") {
            @Override public boolean shouldCreateStub(@Nonnull ASTNode node) { return createStubIfParentIsStub(node); }
            @Nonnull @Override public RsTraitTypeStub deserialize(@Nonnull StubInputStream ds, StubElement p) throws IOException {
                return new RsTraitTypeStub(p, this, ds.readBoolean());
            }
            @Override public void serialize(@Nonnull RsTraitTypeStub s, @Nonnull StubOutputStream ds) throws IOException {
                ds.writeBoolean(s.isImpl);
            }
            @Nonnull @Override public RsTraitType createPsi(@Nonnull RsTraitTypeStub s) { return new RsTraitTypeImpl(s, this); }
            @Nonnull @Override public RsTraitTypeStub createStub(@Nonnull RsTraitType psi, @Nullable StubElement p) {
                return new RsTraitTypeStub(p, this, RsTraitTypeExtUtil.isImpl(psi));
            }
        };
}
