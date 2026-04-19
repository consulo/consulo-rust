/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */
package org.rust.lang.core.stubs;

import com.intellij.psi.stubs.*;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.rust.lang.core.psi.RsForeignModItem;
import org.rust.lang.core.psi.impl.RsForeignModItemImpl;
import java.io.IOException;

public class RsForeignModStub extends RsAttrProcMacroOwnerStubBase<RsForeignModItem> {
    private final int flags;
    @Nullable private final RsProcMacroStubInfo procMacroInfo;
    @Nullable private final String abi;

    public RsForeignModStub(@Nullable StubElement<?> parent, @NotNull IStubElementType<?, ?> elementType,
                             int flags, @Nullable RsProcMacroStubInfo procMacroInfo, @Nullable String abi) {
        super(parent, elementType);
        this.flags = flags; this.procMacroInfo = procMacroInfo; this.abi = abi;
    }
    @Override protected int getFlags() { return flags; }
    @Nullable @Override public RsProcMacroStubInfo getProcMacroInfo() { return procMacroInfo; }
    @Nullable public String getAbi() { return abi; }

    public static final RsStubElementType<RsForeignModStub, RsForeignModItem> Type =
        new RsStubElementType<RsForeignModStub, RsForeignModItem>("FOREIGN_MOD_ITEM") {
            @NotNull @Override public RsForeignModStub deserialize(@NotNull StubInputStream ds, StubElement p) throws IOException {
                return new RsForeignModStub(p, this, ds.readUnsignedByte(), RsProcMacroStubInfo.deserialize(ds), ds.readNameString());
            }
            @Override public void serialize(@NotNull RsForeignModStub s, @NotNull StubOutputStream ds) throws IOException {
                ds.writeByte(s.flags); RsProcMacroStubInfo.serialize(s.procMacroInfo, ds); ds.writeName(s.abi);
            }
            @NotNull @Override public RsForeignModItem createPsi(@NotNull RsForeignModStub s) { return new RsForeignModItemImpl(s, this); }
            @NotNull @Override public RsForeignModStub createStub(@NotNull RsForeignModItem psi, @Nullable StubElement<?> p) {
                int flags = RsAttributeOwnerStub.extractFlags(psi);
                return new RsForeignModStub(p, this, flags, RsAttrProcMacroOwnerStub.extractTextAndOffset(flags, psi), psi.getExternAbi() != null ? psi.getExternAbi().getText() : null);
            }
        };
}
