/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.lang.core.stubs;

import consulo.language.psi.stub.*;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import org.rust.lang.core.psi.*;

import java.io.IOException;
import org.rust.lang.core.psi.impl.*;
import org.rust.lang.core.psi.impl.RsEnumItemImpl;

public class RsEnumItemStub extends RsAttrProcMacroOwnerStubBase<RsEnumItem> implements RsNamedStub {
    @Nullable private final String name;
    private final int flags;
    @Nullable private final RsProcMacroStubInfo procMacroInfo;

    public RsEnumItemStub(@Nullable StubElement parent, @Nonnull IStubElementType elementType,
                           @Nullable String name, int flags, @Nullable RsProcMacroStubInfo procMacroInfo) {
        super(parent, elementType);
        this.name = name;
        this.flags = flags;
        this.procMacroInfo = procMacroInfo;
    }

    @Nullable @Override public String getName() { return name; }
    @Override protected int getFlags() { return flags; }
    @Nullable @Override public RsProcMacroStubInfo getProcMacroInfo() { return procMacroInfo; }

    @SuppressWarnings("unchecked") @Nullable
    public StubElement<RsEnumBody> getEnumBody() { return (StubElement<RsEnumBody>) findChildStubByType(RsStubElementTypes.ENUM_BODY); }


    public static final RsStubElementType<RsEnumItemStub, RsEnumItem> Type =
        new RsStubElementType<RsEnumItemStub, RsEnumItem>("ENUM_ITEM") {
            @Nonnull @Override
            public RsEnumItemStub deserialize(@Nonnull StubInputStream dataStream, StubElement parentStub) throws IOException {
                return new RsEnumItemStub(parentStub, this,
                    StubImplementationsKt.readNameAsString(dataStream), dataStream.readUnsignedByte(), RsProcMacroStubInfo.deserialize(dataStream));
            }
            @Override public void serialize(@Nonnull RsEnumItemStub stub, @Nonnull StubOutputStream dataStream) throws IOException {
                dataStream.writeName(stub.name); dataStream.writeByte(stub.flags); RsProcMacroStubInfo.serialize(stub.procMacroInfo, dataStream);
            }
            @Nonnull @Override public RsEnumItem createPsi(@Nonnull RsEnumItemStub stub) { return new RsEnumItemImpl(stub, this); }
            @Nonnull @Override public RsEnumItemStub createStub(@Nonnull RsEnumItem psi, @Nullable StubElement parentStub) {
                int flags = RsAttributeOwnerStub.extractFlags(psi);
                return new RsEnumItemStub(parentStub, this, psi.getName(), flags, RsAttrProcMacroOwnerStub.extractTextAndOffset(flags, psi));
            }
            @Override public void indexStub(@Nonnull RsEnumItemStub stub, @Nonnull IndexSink sink) { StubIndexing.indexEnumItem(sink, stub); }
        };
}
