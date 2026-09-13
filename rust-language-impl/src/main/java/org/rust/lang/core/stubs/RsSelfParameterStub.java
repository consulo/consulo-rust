/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */
package org.rust.lang.core.stubs;

import consulo.language.psi.stub.*;
import consulo.util.lang.BitUtil;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import org.rust.lang.core.psi.RsSelfParameter;
import org.rust.stdext.CollectionsUtil;
import java.io.IOException;
import org.rust.lang.core.psi.ext.impl.RsSelfParameterUtil;
import org.rust.lang.core.psi.ext.impl.RsSelfParameterUtil;
import org.rust.lang.core.psi.impl.RsSelfParameterImpl;

public class RsSelfParameterStub extends RsAttributeOwnerStubBase<RsSelfParameter> {
    public final int flags;

    static final int IS_MUT_MASK;
    static final int IS_REF_MASK;
    static final int IS_EXPLICIT_TYPE_MASK;

    static {
        // Replicate BitFlagsBuilder(CommonStubAttrFlags, BYTE) logic
        // CommonStubAttrFlags uses 5 bits (0-4), so we start from bit 5
        IS_MUT_MASK = CollectionsUtil.makeBitMask(5);
        IS_REF_MASK = CollectionsUtil.makeBitMask(6);
        IS_EXPLICIT_TYPE_MASK = CollectionsUtil.makeBitMask(7);
    }

    public RsSelfParameterStub(@Nullable StubElement parent, @Nonnull IStubElementType elementType, int flags) {
        super(parent, elementType); this.flags = flags;
    }
    @Override protected int getFlags() { return flags; }
    public boolean isMut() { return BitUtil.isSet(flags, IS_MUT_MASK); }
    public boolean isRef() { return BitUtil.isSet(flags, IS_REF_MASK); }
    public boolean isExplicitType() { return BitUtil.isSet(flags, IS_EXPLICIT_TYPE_MASK); }


    public static final RsStubElementType<RsSelfParameterStub, RsSelfParameter> Type =
        new RsStubElementType<RsSelfParameterStub, RsSelfParameter>("SELF_PARAMETER") {
            @Nonnull @Override public RsSelfParameterStub deserialize(@Nonnull StubInputStream ds, StubElement p) throws IOException {
                return new RsSelfParameterStub(p, this, ds.readVarInt());
            }
            @Override public void serialize(@Nonnull RsSelfParameterStub s, @Nonnull StubOutputStream ds) throws IOException {
                ds.writeVarInt(s.flags);
            }
            @Nonnull @Override public RsSelfParameter createPsi(@Nonnull RsSelfParameterStub s) { return new RsSelfParameterImpl(s, this); }
            @Nonnull @Override public RsSelfParameterStub createStub(@Nonnull RsSelfParameter psi, @Nullable StubElement p) {
                int flags = RsAttributeOwnerStub.extractFlags(psi);
                flags = BitUtil.set(flags, IS_MUT_MASK, RsSelfParameterUtil.getMutability(psi).isMut());
                flags = BitUtil.set(flags, IS_REF_MASK, RsSelfParameterUtil.isRef(psi));
                flags = BitUtil.set(flags, IS_EXPLICIT_TYPE_MASK, RsSelfParameterUtil.isExplicitType(psi));
                return new RsSelfParameterStub(p, this, flags);
            }
        };
}
