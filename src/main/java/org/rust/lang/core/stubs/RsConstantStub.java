/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */
package org.rust.lang.core.stubs;

import com.intellij.psi.stubs.*;
import com.intellij.util.BitUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.rust.lang.core.psi.RsConstant;
import org.rust.lang.core.psi.impl.RsConstantImpl;
import org.rust.stdext.BitFlagsBuilder;
import java.io.IOException;
import org.rust.lang.core.psi.ext.RsConstantUtil;

public class RsConstantStub extends RsAttrProcMacroOwnerStubBase<RsConstant> implements RsNamedStub {
    @Nullable private final String name;
    private final int flags;
    @Nullable private final RsProcMacroStubInfo procMacroInfo;
    private static final int IS_MUT_MASK;
    private static final int IS_CONST_MASK;
    static {
        BitFlagsBuilder b = new BitFlagsBuilder(RsAttributeOwnerStub.CommonStubAttrFlags.INSTANCE, BitFlagsBuilder.Limit.BYTE);
        IS_MUT_MASK = b.nextBitMask(); IS_CONST_MASK = b.nextBitMask();
    }

    public RsConstantStub(@Nullable StubElement<?> parent, @NotNull IStubElementType<?, ?> elementType,
                           @Nullable String name, int flags, @Nullable RsProcMacroStubInfo procMacroInfo) {
        super(parent, elementType);
        this.name = name; this.flags = flags; this.procMacroInfo = procMacroInfo;
    }
    @Nullable @Override public String getName() { return name; }
    @Override protected int getFlags() { return flags; }
    @Nullable @Override public RsProcMacroStubInfo getProcMacroInfo() { return procMacroInfo; }
    public boolean isMut() { return BitUtil.isSet(flags, IS_MUT_MASK); }
    public boolean isConst() { return BitUtil.isSet(flags, IS_CONST_MASK); }

    public static final RsStubElementType<RsConstantStub, RsConstant> Type =
        new RsStubElementType<RsConstantStub, RsConstant>("CONSTANT") {
            @NotNull @Override public RsConstantStub deserialize(@NotNull StubInputStream ds, StubElement p) throws IOException {
                return new RsConstantStub(p, this, StubImplementationsKt.readNameAsString(ds), ds.readUnsignedByte(), RsProcMacroStubInfo.deserialize(ds));
            }
            @Override public void serialize(@NotNull RsConstantStub s, @NotNull StubOutputStream ds) throws IOException {
                ds.writeName(s.name); ds.writeByte(s.flags); RsProcMacroStubInfo.serialize(s.procMacroInfo, ds);
            }
            @NotNull @Override public RsConstant createPsi(@NotNull RsConstantStub s) { return new RsConstantImpl(s, this); }
            @NotNull @Override public RsConstantStub createStub(@NotNull RsConstant psi, @Nullable StubElement<?> p) {
                int flags = RsAttributeOwnerStub.extractFlags(psi);
                flags = BitUtil.set(flags, IS_MUT_MASK, RsConstantUtil.isMut(psi));
                flags = BitUtil.set(flags, IS_CONST_MASK, RsConstantUtil.isConst(psi));
                return new RsConstantStub(p, this, psi.getName(), flags, RsAttrProcMacroOwnerStub.extractTextAndOffset(flags, psi));
            }
            @Override public void indexStub(@NotNull RsConstantStub s, @NotNull IndexSink sink) { StubIndexing.indexConstant(sink, s); }
        };
}
