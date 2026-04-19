/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */
package org.rust.lang.core.stubs;

import com.intellij.psi.stubs.*;
import com.intellij.util.BitUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.rust.lang.core.psi.MacroBraces;
import org.rust.lang.core.psi.RsMacro2;
import org.rust.lang.core.psi.ext.RsMacro2Util;
import org.rust.lang.core.psi.impl.RsMacro2Impl;
import org.rust.lang.core.stubs.RsAttributeOwnerStub.Macro2StubAttrFlags;
import org.rust.stdext.HashCode;
import org.rust.stdext.IoUtil;
import java.io.IOException;

public class RsMacro2Stub extends RsAttrProcMacroOwnerStubBase<RsMacro2> implements RsNamedStub {
    @Nullable private final String name;
    @NotNull private final String macroBody;
    @NotNull private final HashCode bodyHash;
    @NotNull private final MacroBraces preferredBraces;
    private final int flags;
    @Nullable private final RsProcMacroStubInfo procMacroInfo;

    public RsMacro2Stub(@Nullable StubElement<?> parent, @NotNull IStubElementType<?, ?> elementType,
                         @Nullable String name, @NotNull String macroBody, @NotNull HashCode bodyHash,
                         @NotNull MacroBraces preferredBraces, int flags, @Nullable RsProcMacroStubInfo procMacroInfo) {
        super(parent, elementType);
        this.name = name; this.macroBody = macroBody; this.bodyHash = bodyHash;
        this.preferredBraces = preferredBraces; this.flags = flags; this.procMacroInfo = procMacroInfo;
    }
    @Nullable @Override public String getName() { return name; }
    @NotNull public String getMacroBody() { return macroBody; }
    @NotNull public HashCode getBodyHash() { return bodyHash; }
    @NotNull public MacroBraces getPreferredBraces() { return preferredBraces; }
    @Override protected int getFlags() { return flags; }
    @Nullable @Override public RsProcMacroStubInfo getProcMacroInfo() { return procMacroInfo; }

    public boolean getMayHaveRustcBuiltinMacro() { return BitUtil.isSet(flags, Macro2StubAttrFlags.MAY_HAVE_RUSTC_BUILTIN_MACRO); }

    public static final RsStubElementType<RsMacro2Stub, RsMacro2> Type =
        new RsStubElementType<RsMacro2Stub, RsMacro2>("MACRO_2") {
            @NotNull @Override public RsMacro2Stub deserialize(@NotNull StubInputStream ds, StubElement p) throws IOException {
                return new RsMacro2Stub(p, this,
                    StubImplementationsKt.readNameAsString(ds),
                    ds.readUTFFast(),
                    HashCode.readHashCode(ds),
                    IoUtil.readEnum(ds, MacroBraces.class),
                    ds.readUnsignedByte(),
                    RsProcMacroStubInfo.deserialize(ds));
            }
            @Override public void serialize(@NotNull RsMacro2Stub s, @NotNull StubOutputStream ds) throws IOException {
                ds.writeName(s.name);
                ds.writeUTFFast(s.macroBody);
                HashCode.writeHashCode(ds, s.bodyHash);
                IoUtil.writeEnum(ds, s.preferredBraces);
                ds.writeByte(s.flags);
                RsProcMacroStubInfo.serialize(s.procMacroInfo, ds);
            }
            @NotNull @Override public RsMacro2 createPsi(@NotNull RsMacro2Stub s) { return new RsMacro2Impl(s, this); }
            @NotNull @Override public RsMacro2Stub createStub(@NotNull RsMacro2 psi, @Nullable StubElement<?> p) {
                int flags = RsAttributeOwnerStub.extractFlags(psi, new Macro2StubAttrFlags());
                MacroBraces preferredBraces = psi.getPreferredBraces();
                String body = RsMacro2Util.prepareMacroBody(psi);
                RsProcMacroStubInfo procMacroInfo = RsAttrProcMacroOwnerStub.extractTextAndOffset(flags, psi);
                return new RsMacro2Stub(p, this, psi.getName(), body, HashCode.compute(body), preferredBraces, flags, procMacroInfo);
            }
            @Override public void indexStub(@NotNull RsMacro2Stub s, @NotNull IndexSink sink) { StubIndexing.indexMacroDef(sink, s); }
        };
}
