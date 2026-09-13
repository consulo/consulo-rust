/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.lang.core.stubs;

import consulo.language.psi.stub.*;
import consulo.util.lang.BitUtil;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import org.rust.lang.core.psi.RsImplItem;
import org.rust.stdext.BitFlagsBuilder;
import java.io.IOException;
import org.rust.lang.core.psi.ext.impl.RsImplItemUtil;
import org.rust.lang.core.psi.impl.RsImplItemImpl;

public class RsImplItemStub extends RsAttrProcMacroOwnerStubBase<RsImplItem> {
    private final int flags;
    @Nullable private final RsProcMacroStubInfo procMacroInfo;

    static final int NEGATIVE_IMPL_MASK;
    static {
        BitFlagsBuilder b = new BitFlagsBuilder(RsAttributeOwnerStub.ImplStubAttrFlags.INSTANCE, BitFlagsBuilder.Limit.BYTE);
        NEGATIVE_IMPL_MASK = b.nextBitMask();
    }

    public RsImplItemStub(@Nullable StubElement parent, @Nonnull IStubElementType elementType,
                           int flags, @Nullable RsProcMacroStubInfo procMacroInfo) {
        super(parent, elementType);
        this.flags = flags; this.procMacroInfo = procMacroInfo;
    }

    @Override protected int getFlags() { return flags; }
    @Nullable @Override public RsProcMacroStubInfo getProcMacroInfo() { return procMacroInfo; }
    public boolean getMayBeReservationImpl() { return BitUtil.isSet(flags, RsAttributeOwnerStub.ImplStubAttrFlags.MAY_BE_RESERVATION_IMPL); }
    public boolean isNegativeImpl() { return BitUtil.isSet(flags, NEGATIVE_IMPL_MASK); }


    public static final RsStubElementType<RsImplItemStub, RsImplItem> Type =
        new RsStubElementType<RsImplItemStub, RsImplItem>("IMPL_ITEM") {
            @Nonnull @Override
            public RsImplItemStub deserialize(@Nonnull StubInputStream dataStream, StubElement parentStub) throws IOException {
                return new RsImplItemStub(parentStub, this, dataStream.readUnsignedByte(), RsProcMacroStubInfo.deserialize(dataStream));
            }
            @Override public void serialize(@Nonnull RsImplItemStub stub, @Nonnull StubOutputStream dataStream) throws IOException {
                dataStream.writeByte(stub.flags); RsProcMacroStubInfo.serialize(stub.procMacroInfo, dataStream);
            }
            @Nonnull @Override public RsImplItem createPsi(@Nonnull RsImplItemStub stub) { return new RsImplItemImpl(stub, this); }
            @Nonnull @Override public RsImplItemStub createStub(@Nonnull RsImplItem psi, @Nullable StubElement parentStub) {
                int flags = RsAttributeOwnerStub.extractFlags(psi, new RsAttributeOwnerStub.ImplStubAttrFlags());
                flags = BitUtil.set(flags, NEGATIVE_IMPL_MASK, RsImplItemUtil.isNegativeImpl(psi));
                return new RsImplItemStub(parentStub, this, flags, RsAttrProcMacroOwnerStub.extractTextAndOffset(flags, psi));
            }
            @Override public void indexStub(@Nonnull RsImplItemStub stub, @Nonnull IndexSink sink) { StubIndexing.indexImplItem(sink, stub); }
        };
}
