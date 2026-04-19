/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.lang.core.stubs;

import com.intellij.psi.stubs.*;
import com.intellij.util.BitUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.rust.lang.core.psi.RsModItem;
import org.rust.lang.core.psi.impl.RsModItemImpl;
import static org.rust.lang.core.stubs.RsAttributeOwnerStub.ModStubAttrFlags.MAY_HAVE_MACRO_USE;
import java.io.IOException;

public class RsModItemStub extends RsAttrProcMacroOwnerStubBase<RsModItem> implements RsNamedStub {
    @Nullable private final String name;
    private final int flags;
    @Nullable private final RsProcMacroStubInfo procMacroInfo;

    public RsModItemStub(@Nullable StubElement<?> parent, @NotNull IStubElementType<?, ?> elementType,
                          @Nullable String name, int flags, @Nullable RsProcMacroStubInfo procMacroInfo) {
        super(parent, elementType);
        this.name = name; this.flags = flags; this.procMacroInfo = procMacroInfo;
    }

    @Nullable @Override public String getName() { return name; }
    @Override protected int getFlags() { return flags; }
    @Nullable @Override public RsProcMacroStubInfo getProcMacroInfo() { return procMacroInfo; }
    public boolean getMayHaveMacroUse() { return BitUtil.isSet(flags, MAY_HAVE_MACRO_USE); }

    public static final RsStubElementType<RsModItemStub, RsModItem> Type =
        new RsStubElementType<RsModItemStub, RsModItem>("MOD_ITEM") {
            @NotNull @Override
            public RsModItemStub deserialize(@NotNull StubInputStream dataStream, StubElement parentStub) throws IOException {
                return new RsModItemStub(parentStub, this, StubImplementationsKt.readNameAsString(dataStream), dataStream.readUnsignedByte(), RsProcMacroStubInfo.deserialize(dataStream));
            }
            @Override public void serialize(@NotNull RsModItemStub stub, @NotNull StubOutputStream dataStream) throws IOException {
                dataStream.writeName(stub.name); dataStream.writeByte(stub.flags); RsProcMacroStubInfo.serialize(stub.procMacroInfo, dataStream);
            }
            @NotNull @Override public RsModItem createPsi(@NotNull RsModItemStub stub) { return new RsModItemImpl(stub, this); }
            @NotNull @Override public RsModItemStub createStub(@NotNull RsModItem psi, @Nullable StubElement<?> parentStub) {
                int flags = RsAttributeOwnerStub.extractFlags(psi, new RsAttributeOwnerStub.ModStubAttrFlags());
                return new RsModItemStub(parentStub, this, psi.getName(), flags, RsAttrProcMacroOwnerStub.extractTextAndOffset(flags, psi));
            }
            @Override public void indexStub(@NotNull RsModItemStub stub, @NotNull IndexSink sink) { StubIndexing.indexModItem(sink, stub); }
        };
}
