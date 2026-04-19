/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */
package org.rust.lang.core.stubs;

import com.intellij.psi.stubs.*;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.rust.lang.core.psi.RsTraitAlias;
import org.rust.lang.core.psi.impl.RsTraitAliasImpl;
import java.io.IOException;

public class RsTraitAliasStub extends RsAttrProcMacroOwnerStubBase<RsTraitAlias> implements RsNamedStub {
    @Nullable private final String name;
    private final int flags;
    @Nullable private final RsProcMacroStubInfo procMacroInfo;

    public RsTraitAliasStub(@Nullable StubElement<?> parent, @NotNull IStubElementType<?, ?> elementType,
                             @Nullable String name, int flags, @Nullable RsProcMacroStubInfo procMacroInfo) {
        super(parent, elementType);
        this.name = name; this.flags = flags; this.procMacroInfo = procMacroInfo;
    }
    @Nullable @Override public String getName() { return name; }
    @Override protected int getFlags() { return flags; }
    @Nullable @Override public RsProcMacroStubInfo getProcMacroInfo() { return procMacroInfo; }

    public static final RsStubElementType<RsTraitAliasStub, RsTraitAlias> Type =
        new RsStubElementType<RsTraitAliasStub, RsTraitAlias>("TRAIT_ALIAS") {
            @NotNull @Override public RsTraitAliasStub deserialize(@NotNull StubInputStream ds, StubElement p) throws IOException {
                return new RsTraitAliasStub(p, this, StubImplementationsKt.readNameAsString(ds), ds.readUnsignedByte(), RsProcMacroStubInfo.deserialize(ds));
            }
            @Override public void serialize(@NotNull RsTraitAliasStub s, @NotNull StubOutputStream ds) throws IOException {
                ds.writeName(s.name); ds.writeByte(s.flags); RsProcMacroStubInfo.serialize(s.procMacroInfo, ds);
            }
            @NotNull @Override public RsTraitAlias createPsi(@NotNull RsTraitAliasStub s) { return new RsTraitAliasImpl(s, this); }
            @NotNull @Override public RsTraitAliasStub createStub(@NotNull RsTraitAlias psi, @Nullable StubElement<?> p) {
                int flags = RsAttributeOwnerStub.extractFlags(psi);
                return new RsTraitAliasStub(p, this, psi.getName(), flags, RsAttrProcMacroOwnerStub.extractTextAndOffset(flags, psi));
            }
            @Override public void indexStub(@NotNull RsTraitAliasStub s, @NotNull IndexSink sink) { StubIndexing.indexTraitAlias(sink, s); }
        };
}
