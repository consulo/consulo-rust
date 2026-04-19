/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */
package org.rust.lang.core.stubs;

import com.intellij.psi.stubs.*;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.rust.lang.core.psi.RsTypeAlias;
import org.rust.lang.core.psi.impl.RsTypeAliasImpl;
import java.io.IOException;

public class RsTypeAliasStub extends RsAttrProcMacroOwnerStubBase<RsTypeAlias> implements RsNamedStub {
    @Nullable private final String name;
    private final int flags;
    @Nullable private final RsProcMacroStubInfo procMacroInfo;

    public RsTypeAliasStub(@Nullable StubElement<?> parent, @NotNull IStubElementType<?, ?> elementType,
                            @Nullable String name, int flags, @Nullable RsProcMacroStubInfo procMacroInfo) {
        super(parent, elementType);
        this.name = name; this.flags = flags; this.procMacroInfo = procMacroInfo;
    }
    @Nullable @Override public String getName() { return name; }
    @Override protected int getFlags() { return flags; }
    @Nullable @Override public RsProcMacroStubInfo getProcMacroInfo() { return procMacroInfo; }

    public static final RsStubElementType<RsTypeAliasStub, RsTypeAlias> Type =
        new RsStubElementType<RsTypeAliasStub, RsTypeAlias>("TYPE_ALIAS") {
            @NotNull @Override public RsTypeAliasStub deserialize(@NotNull StubInputStream ds, StubElement p) throws IOException {
                return new RsTypeAliasStub(p, this, StubImplementationsKt.readNameAsString(ds), ds.readUnsignedByte(), RsProcMacroStubInfo.deserialize(ds));
            }
            @Override public void serialize(@NotNull RsTypeAliasStub s, @NotNull StubOutputStream ds) throws IOException {
                ds.writeName(s.name); ds.writeByte(s.flags); RsProcMacroStubInfo.serialize(s.procMacroInfo, ds);
            }
            @NotNull @Override public RsTypeAlias createPsi(@NotNull RsTypeAliasStub s) { return new RsTypeAliasImpl(s, this); }
            @NotNull @Override public RsTypeAliasStub createStub(@NotNull RsTypeAlias psi, @Nullable StubElement<?> p) {
                int flags = RsAttributeOwnerStub.extractFlags(psi);
                return new RsTypeAliasStub(p, this, psi.getName(), flags, RsAttrProcMacroOwnerStub.extractTextAndOffset(flags, psi));
            }
            @Override public void indexStub(@NotNull RsTypeAliasStub s, @NotNull IndexSink sink) { StubIndexing.indexTypeAlias(sink, s); }
        };
}
