/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.lang.core.stubs;

import com.intellij.psi.stubs.*;
import com.intellij.util.BitUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.rust.lang.core.psi.RsTraitItem;
import org.rust.lang.core.psi.impl.RsTraitItemImpl;
import org.rust.stdext.BitFlagsBuilder;
import java.io.IOException;

public class RsTraitItemStub extends RsAttrProcMacroOwnerStubBase<RsTraitItem> implements RsNamedStub {
    @Nullable private final String name;
    private final int flags;
    @Nullable private final RsProcMacroStubInfo procMacroInfo;

    private static final int UNSAFE_MASK;
    private static final int AUTO_MASK;
    static {
        BitFlagsBuilder b = new BitFlagsBuilder(RsAttributeOwnerStub.CommonStubAttrFlags.INSTANCE, BitFlagsBuilder.Limit.BYTE);
        UNSAFE_MASK = b.nextBitMask();
        AUTO_MASK = b.nextBitMask();
    }

    public RsTraitItemStub(@Nullable StubElement<?> parent, @NotNull IStubElementType<?, ?> elementType,
                            @Nullable String name, int flags, @Nullable RsProcMacroStubInfo procMacroInfo) {
        super(parent, elementType);
        this.name = name; this.flags = flags; this.procMacroInfo = procMacroInfo;
    }

    @Nullable @Override public String getName() { return name; }
    @Override protected int getFlags() { return flags; }
    @Nullable @Override public RsProcMacroStubInfo getProcMacroInfo() { return procMacroInfo; }
    public boolean isUnsafe() { return BitUtil.isSet(flags, UNSAFE_MASK); }
    public boolean isAuto() { return BitUtil.isSet(flags, AUTO_MASK); }

    public static final RsStubElementType<RsTraitItemStub, RsTraitItem> Type =
        new RsStubElementType<RsTraitItemStub, RsTraitItem>("TRAIT_ITEM") {
            @NotNull @Override
            public RsTraitItemStub deserialize(@NotNull StubInputStream dataStream, StubElement parentStub) throws IOException {
                return new RsTraitItemStub(parentStub, this, StubImplementationsKt.readNameAsString(dataStream), dataStream.readUnsignedByte(), RsProcMacroStubInfo.deserialize(dataStream));
            }
            @Override public void serialize(@NotNull RsTraitItemStub stub, @NotNull StubOutputStream dataStream) throws IOException {
                dataStream.writeName(stub.name); dataStream.writeByte(stub.flags); RsProcMacroStubInfo.serialize(stub.procMacroInfo, dataStream);
            }
            @NotNull @Override public RsTraitItem createPsi(@NotNull RsTraitItemStub stub) { return new RsTraitItemImpl(stub, this); }
            @NotNull @Override public RsTraitItemStub createStub(@NotNull RsTraitItem psi, @Nullable StubElement<?> parentStub) {
                int flags = RsAttributeOwnerStub.extractFlags(psi);
                flags = BitUtil.set(flags, UNSAFE_MASK, psi.isUnsafe());
                flags = BitUtil.set(flags, AUTO_MASK, psi.getNode().findChildByType(org.rust.lang.core.psi.RsElementTypes.AUTO) != null);
                return new RsTraitItemStub(parentStub, this, psi.getName(), flags, RsAttrProcMacroOwnerStub.extractTextAndOffset(flags, psi));
            }
            @Override public void indexStub(@NotNull RsTraitItemStub stub, @NotNull IndexSink sink) { StubIndexing.indexTraitItem(sink, stub); }
        };
}
