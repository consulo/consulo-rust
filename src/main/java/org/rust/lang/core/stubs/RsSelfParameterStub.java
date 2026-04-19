/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */
package org.rust.lang.core.stubs;

import com.intellij.psi.stubs.*;
import com.intellij.util.BitUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.rust.lang.core.psi.RsSelfParameter;
import org.rust.lang.core.psi.impl.RsSelfParameterImpl;
import org.rust.lang.core.stubs.RsAttributeOwnerStub.CommonStubAttrFlags;
import org.rust.stdext.BitFlagsBuilder;
import org.rust.stdext.CollectionsUtil;
import java.io.IOException;
import org.rust.lang.core.psi.ext.RsSelfParameterUtil;
import org.rust.lang.core.psi.ext.RsSelfParameterUtil;

public class RsSelfParameterStub extends RsAttributeOwnerStubBase<RsSelfParameter> {
    private final int flags;

    private static final int IS_MUT_MASK;
    private static final int IS_REF_MASK;
    private static final int IS_EXPLICIT_TYPE_MASK;

    static {
        // Replicate BitFlagsBuilder(CommonStubAttrFlags, BYTE) logic
        // CommonStubAttrFlags uses 5 bits (0-4), so we start from bit 5
        IS_MUT_MASK = CollectionsUtil.makeBitMask(5);
        IS_REF_MASK = CollectionsUtil.makeBitMask(6);
        IS_EXPLICIT_TYPE_MASK = CollectionsUtil.makeBitMask(7);
    }

    public RsSelfParameterStub(@Nullable StubElement<?> parent, @NotNull IStubElementType<?, ?> elementType, int flags) {
        super(parent, elementType); this.flags = flags;
    }
    @Override protected int getFlags() { return flags; }
    public boolean isMut() { return BitUtil.isSet(flags, IS_MUT_MASK); }
    public boolean isRef() { return BitUtil.isSet(flags, IS_REF_MASK); }
    public boolean isExplicitType() { return BitUtil.isSet(flags, IS_EXPLICIT_TYPE_MASK); }

    public static final RsStubElementType<RsSelfParameterStub, RsSelfParameter> Type =
        new RsStubElementType<RsSelfParameterStub, RsSelfParameter>("SELF_PARAMETER") {
            @NotNull @Override public RsSelfParameterStub deserialize(@NotNull StubInputStream ds, StubElement p) throws IOException {
                return new RsSelfParameterStub(p, this, ds.readVarInt());
            }
            @Override public void serialize(@NotNull RsSelfParameterStub s, @NotNull StubOutputStream ds) throws IOException {
                ds.writeVarInt(s.flags);
            }
            @NotNull @Override public RsSelfParameter createPsi(@NotNull RsSelfParameterStub s) { return new RsSelfParameterImpl(s, this); }
            @NotNull @Override public RsSelfParameterStub createStub(@NotNull RsSelfParameter psi, @Nullable StubElement<?> p) {
                int flags = RsAttributeOwnerStub.extractFlags(psi);
                flags = BitUtil.set(flags, IS_MUT_MASK, RsSelfParameterUtil.getMutability(psi).isMut());
                flags = BitUtil.set(flags, IS_REF_MASK, RsSelfParameterUtil.isRef(psi));
                flags = BitUtil.set(flags, IS_EXPLICIT_TYPE_MASK, RsSelfParameterUtil.isExplicitType(psi));
                return new RsSelfParameterStub(p, this, flags);
            }
        };
}
