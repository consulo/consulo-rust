/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.lang.core.stubs;

import com.intellij.psi.stubs.*;
import com.intellij.util.BitUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.rust.lang.core.psi.RsUseItem;
import org.rust.lang.core.psi.impl.RsUseItemImpl;

import java.io.IOException;

import static org.rust.lang.core.stubs.RsAttributeOwnerStub.UseItemStubAttrFlags.MAY_HAVE_PRELUDE_IMPORT;

public class RsUseItemStub extends RsAttrProcMacroOwnerStubBase<RsUseItem> {
    private final int flags;
    @Nullable private final RsProcMacroStubInfo procMacroInfo;

    public RsUseItemStub(@Nullable StubElement<?> parent, @NotNull IStubElementType<?, ?> elementType,
                          int flags, @Nullable RsProcMacroStubInfo procMacroInfo) {
        super(parent, elementType);
        this.flags = flags;
        this.procMacroInfo = procMacroInfo;
    }

    @Override protected int getFlags() { return flags; }
    @Nullable @Override public RsProcMacroStubInfo getProcMacroInfo() { return procMacroInfo; }

    @Nullable
    public RsUseSpeckStub getUseSpeck() { return (RsUseSpeckStub) findChildStubByType(RsUseSpeckStub.Type); }

    public boolean getMayHavePreludeImport() { return BitUtil.isSet(flags, MAY_HAVE_PRELUDE_IMPORT); }

    public static final RsStubElementType<RsUseItemStub, RsUseItem> Type =
        new RsStubElementType<RsUseItemStub, RsUseItem>("USE_ITEM") {
            @NotNull @Override
            public RsUseItemStub deserialize(@NotNull StubInputStream dataStream, StubElement parentStub) throws IOException {
                return new RsUseItemStub(parentStub, this, dataStream.readUnsignedByte(), RsProcMacroStubInfo.deserialize(dataStream));
            }

            @Override
            public void serialize(@NotNull RsUseItemStub stub, @NotNull StubOutputStream dataStream) throws IOException {
                dataStream.writeByte(stub.flags);
                RsProcMacroStubInfo.serialize(stub.procMacroInfo, dataStream);
            }

            @NotNull @Override
            public RsUseItem createPsi(@NotNull RsUseItemStub stub) { return new RsUseItemImpl(stub, this); }

            @NotNull @Override
            public RsUseItemStub createStub(@NotNull RsUseItem psi, @Nullable StubElement<?> parentStub) {
                int flags = RsAttributeOwnerStub.extractFlags(psi, new RsAttributeOwnerStub.UseItemStubAttrFlags());
                RsProcMacroStubInfo procMacroInfo = RsAttrProcMacroOwnerStub.extractTextAndOffset(flags, psi);
                return new RsUseItemStub(parentStub, this, flags, procMacroInfo);
            }
        };
}
