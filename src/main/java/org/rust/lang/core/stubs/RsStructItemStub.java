/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.lang.core.stubs;

import com.intellij.lang.ASTNode;
import com.intellij.psi.stubs.*;
import com.intellij.util.BitUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.rust.lang.core.psi.*;
import org.rust.lang.core.psi.impl.RsStructItemImpl;
import org.rust.stdext.BitFlagsBuilder;

import java.io.IOException;
import org.rust.lang.core.psi.ext.RsStructItemUtil;
import org.rust.lang.core.psi.ext.RsStructKind;

public class RsStructItemStub extends RsAttrProcMacroOwnerStubBase<RsStructItem> implements RsNamedStub {
    @Nullable private final String name;
    private final int flags;
    @Nullable private final RsProcMacroStubInfo procMacroInfo;

    private static final int IS_UNION_MASK;
    static {
        BitFlagsBuilder builder = new BitFlagsBuilder(RsAttributeOwnerStub.CommonStubAttrFlags.INSTANCE, BitFlagsBuilder.Limit.BYTE);
        IS_UNION_MASK = builder.nextBitMask();
    }

    public RsStructItemStub(@Nullable StubElement<?> parent, @NotNull IStubElementType<?, ?> elementType,
                              @Nullable String name, int flags, @Nullable RsProcMacroStubInfo procMacroInfo) {
        super(parent, elementType);
        this.name = name;
        this.flags = flags;
        this.procMacroInfo = procMacroInfo;
    }

    @Nullable @Override public String getName() { return name; }
    @Override protected int getFlags() { return flags; }
    @Nullable @Override public RsProcMacroStubInfo getProcMacroInfo() { return procMacroInfo; }

    @SuppressWarnings("unchecked")
    @Nullable public StubElement<RsBlockFields> getBlockFields() {
        return (StubElement<RsBlockFields>) findChildStubByType(RsStubElementTypes.BLOCK_FIELDS);
    }
    public boolean isUnion() { return BitUtil.isSet(flags, IS_UNION_MASK); }

    public static final RsStubElementType<RsStructItemStub, RsStructItem> Type =
        new RsStubElementType<RsStructItemStub, RsStructItem>("STRUCT_ITEM") {
            @NotNull @Override
            public RsStructItemStub deserialize(@NotNull StubInputStream dataStream, StubElement parentStub) throws IOException {
                return new RsStructItemStub(parentStub, this,
                    StubImplementationsKt.readNameAsString(dataStream),
                    dataStream.readUnsignedByte(),
                    RsProcMacroStubInfo.deserialize(dataStream));
            }

            @Override
            public void serialize(@NotNull RsStructItemStub stub, @NotNull StubOutputStream dataStream) throws IOException {
                dataStream.writeName(stub.name);
                dataStream.writeByte(stub.flags);
                RsProcMacroStubInfo.serialize(stub.procMacroInfo, dataStream);
            }

            @NotNull @Override
            public RsStructItem createPsi(@NotNull RsStructItemStub stub) { return new RsStructItemImpl(stub, this); }

            @NotNull @Override
            public RsStructItemStub createStub(@NotNull RsStructItem psi, @Nullable StubElement<?> parentStub) {
                int flags = RsAttributeOwnerStub.extractFlags(psi);
                flags = BitUtil.set(flags, IS_UNION_MASK, RsStructItemUtil.getKind(psi) == RsStructKind.UNION);
                RsProcMacroStubInfo procMacroInfo = RsAttrProcMacroOwnerStub.extractTextAndOffset(flags, psi);
                return new RsStructItemStub(parentStub, this, psi.getName(), flags, procMacroInfo);
            }

            @Override
            public void indexStub(@NotNull RsStructItemStub stub, @NotNull IndexSink sink) {
                StubIndexing.indexStructItem(sink, stub);
            }
        };
}
