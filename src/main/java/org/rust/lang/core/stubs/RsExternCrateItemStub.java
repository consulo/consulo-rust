/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.lang.core.stubs;

import com.intellij.psi.stubs.*;
import com.intellij.util.BitUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.rust.lang.core.psi.RsExternCrateItem;
import org.rust.lang.core.psi.impl.RsExternCrateItemImpl;

import java.io.IOException;

import static org.rust.lang.core.stubs.RsAttributeOwnerStub.ModStubAttrFlags.MAY_HAVE_MACRO_USE;

public class RsExternCrateItemStub extends RsAttrProcMacroOwnerStubBase<RsExternCrateItem> implements RsNamedStub {
    @NotNull private final String name;
    private final int flags;
    @Nullable private final RsProcMacroStubInfo procMacroInfo;

    public RsExternCrateItemStub(@Nullable StubElement<?> parent, @NotNull IStubElementType<?, ?> elementType,
                                  @NotNull String name, int flags, @Nullable RsProcMacroStubInfo procMacroInfo) {
        super(parent, elementType);
        this.name = name;
        this.flags = flags;
        this.procMacroInfo = procMacroInfo;
    }

    @Nullable @Override public String getName() { return name; }
    @Override protected int getFlags() { return flags; }
    @Nullable @Override public RsProcMacroStubInfo getProcMacroInfo() { return procMacroInfo; }
    public boolean getMayHaveMacroUse() { return BitUtil.isSet(flags, MAY_HAVE_MACRO_USE); }

    @Nullable
    public RsAliasStub getAlias() { return (RsAliasStub) findChildStubByType(RsAliasStub.Type); }

    public static final RsStubElementType<RsExternCrateItemStub, RsExternCrateItem> Type =
        new RsStubElementType<RsExternCrateItemStub, RsExternCrateItem>("EXTERN_CRATE_ITEM") {
            @NotNull @Override
            public RsExternCrateItemStub deserialize(@NotNull StubInputStream dataStream, StubElement parentStub) throws IOException {
                return new RsExternCrateItemStub(parentStub, this,
                    StubImplementationsKt.readNameAsString(dataStream),
                    dataStream.readUnsignedByte(),
                    RsProcMacroStubInfo.deserialize(dataStream));
            }

            @Override
            public void serialize(@NotNull RsExternCrateItemStub stub, @NotNull StubOutputStream dataStream) throws IOException {
                dataStream.writeName(stub.name);
                dataStream.writeByte(stub.flags);
                RsProcMacroStubInfo.serialize(stub.procMacroInfo, dataStream);
            }

            @NotNull @Override
            public RsExternCrateItem createPsi(@NotNull RsExternCrateItemStub stub) {
                return new RsExternCrateItemImpl(stub, this);
            }

            @NotNull @Override
            public RsExternCrateItemStub createStub(@NotNull RsExternCrateItem psi, @Nullable StubElement<?> parentStub) {
                int flags = RsAttributeOwnerStub.extractFlags(psi, new RsAttributeOwnerStub.ModStubAttrFlags());
                RsProcMacroStubInfo procMacroInfo = RsAttrProcMacroOwnerStub.extractTextAndOffset(flags, psi);
                return new RsExternCrateItemStub(parentStub, this, psi.getReferenceName(), flags, procMacroInfo);
            }

            @Override
            public void indexStub(@NotNull RsExternCrateItemStub stub, @NotNull IndexSink sink) {
                StubIndexing.indexExternCrate(sink, stub);
            }
        };
}
