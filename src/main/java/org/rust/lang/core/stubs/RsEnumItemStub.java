/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.lang.core.stubs;

import com.intellij.psi.stubs.*;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.rust.lang.core.psi.*;
import org.rust.lang.core.psi.impl.RsEnumItemImpl;

import java.io.IOException;

public class RsEnumItemStub extends RsAttrProcMacroOwnerStubBase<RsEnumItem> implements RsNamedStub {
    @Nullable private final String name;
    private final int flags;
    @Nullable private final RsProcMacroStubInfo procMacroInfo;

    public RsEnumItemStub(@Nullable StubElement<?> parent, @NotNull IStubElementType<?, ?> elementType,
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
            @NotNull @Override
            public RsEnumItemStub deserialize(@NotNull StubInputStream dataStream, StubElement parentStub) throws IOException {
                return new RsEnumItemStub(parentStub, this,
                    StubImplementationsKt.readNameAsString(dataStream), dataStream.readUnsignedByte(), RsProcMacroStubInfo.deserialize(dataStream));
            }
            @Override public void serialize(@NotNull RsEnumItemStub stub, @NotNull StubOutputStream dataStream) throws IOException {
                dataStream.writeName(stub.name); dataStream.writeByte(stub.flags); RsProcMacroStubInfo.serialize(stub.procMacroInfo, dataStream);
            }
            @NotNull @Override public RsEnumItem createPsi(@NotNull RsEnumItemStub stub) { return new RsEnumItemImpl(stub, this); }
            @NotNull @Override public RsEnumItemStub createStub(@NotNull RsEnumItem psi, @Nullable StubElement<?> parentStub) {
                int flags = RsAttributeOwnerStub.extractFlags(psi);
                return new RsEnumItemStub(parentStub, this, psi.getName(), flags, RsAttrProcMacroOwnerStub.extractTextAndOffset(flags, psi));
            }
            @Override public void indexStub(@NotNull RsEnumItemStub stub, @NotNull IndexSink sink) { StubIndexing.indexEnumItem(sink, stub); }
        };
}
