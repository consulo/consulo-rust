/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.lang.core.stubs;


import consulo.language.psi.stub.*;
import consulo.util.lang.BitUtil;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import org.rust.lang.core.psi.RsExternCrateItem;

import java.io.IOException;

import static org.rust.lang.core.stubs.RsAttributeOwnerStub.ModStubAttrFlags.MAY_HAVE_MACRO_USE;
import org.rust.lang.core.psi.impl.RsExternCrateItemImpl;

public class RsExternCrateItemStub extends RsAttrProcMacroOwnerStubBase<RsExternCrateItem> implements RsNamedStub {
    @Nonnull private final String name;
    private final int flags;
    @Nullable private final RsProcMacroStubInfo procMacroInfo;

    public RsExternCrateItemStub(@Nullable StubElement parent, @Nonnull IStubElementType elementType,
                                  @Nonnull String name, int flags, @Nullable RsProcMacroStubInfo procMacroInfo) {
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
            @Nonnull @Override
            public RsExternCrateItemStub deserialize(@Nonnull StubInputStream dataStream, StubElement parentStub) throws IOException {
                return new RsExternCrateItemStub(parentStub, this,
                    StubImplementationsKt.readNameAsString(dataStream),
                    dataStream.readUnsignedByte(),
                    RsProcMacroStubInfo.deserialize(dataStream));
            }

            @Override
            public void serialize(@Nonnull RsExternCrateItemStub stub, @Nonnull StubOutputStream dataStream) throws IOException {
                dataStream.writeName(stub.name);
                dataStream.writeByte(stub.flags);
                RsProcMacroStubInfo.serialize(stub.procMacroInfo, dataStream);
            }

            @Nonnull @Override
            public RsExternCrateItem createPsi(@Nonnull RsExternCrateItemStub stub) {
                return new RsExternCrateItemImpl(stub, this);
            }

            @Nonnull @Override
            public RsExternCrateItemStub createStub(@Nonnull RsExternCrateItem psi, @Nullable StubElement parentStub) {
                int flags = RsAttributeOwnerStub.extractFlags(psi, new RsAttributeOwnerStub.ModStubAttrFlags());
                RsProcMacroStubInfo procMacroInfo = RsAttrProcMacroOwnerStub.extractTextAndOffset(flags, psi);
                return new RsExternCrateItemStub(parentStub, this, psi.getReferenceName(), flags, procMacroInfo);
            }

            @Override
            public void indexStub(@Nonnull RsExternCrateItemStub stub, @Nonnull IndexSink sink) {
                StubIndexing.indexExternCrate(sink, stub);
            }
        };
}
