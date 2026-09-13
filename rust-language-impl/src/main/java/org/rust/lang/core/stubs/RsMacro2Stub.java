/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */
package org.rust.lang.core.stubs;


import consulo.language.psi.stub.*;
import consulo.util.lang.BitUtil;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import org.rust.lang.core.psi.MacroBraces;
import org.rust.lang.core.psi.RsMacro2;
import org.rust.lang.core.psi.ext.impl.RsMacro2Util;
import org.rust.lang.core.stubs.RsAttributeOwnerStub.Macro2StubAttrFlags;
import org.rust.stdext.HashCode;
import org.rust.stdext.IoUtil;
import java.io.IOException;
import org.rust.lang.core.psi.impl.RsMacro2Impl;

public class RsMacro2Stub extends RsAttrProcMacroOwnerStubBase<RsMacro2> implements RsNamedStub {
    @Nullable public final String name;
    @Nonnull public final String macroBody;
    @Nonnull public final HashCode bodyHash;
    @Nonnull public final MacroBraces preferredBraces;
    public final int flags;
    @Nullable public final RsProcMacroStubInfo procMacroInfo;

    public RsMacro2Stub(@Nullable StubElement parent, @Nonnull IStubElementType elementType,
                         @Nullable String name, @Nonnull String macroBody, @Nonnull HashCode bodyHash,
                         @Nonnull MacroBraces preferredBraces, int flags, @Nullable RsProcMacroStubInfo procMacroInfo) {
        super(parent, elementType);
        this.name = name; this.macroBody = macroBody; this.bodyHash = bodyHash;
        this.preferredBraces = preferredBraces; this.flags = flags; this.procMacroInfo = procMacroInfo;
    }
    @Nullable @Override public String getName() { return name; }
    @Nonnull public String getMacroBody() { return macroBody; }
    @Nonnull public HashCode getBodyHash() { return bodyHash; }
    @Nonnull public MacroBraces getPreferredBraces() { return preferredBraces; }
    @Override protected int getFlags() { return flags; }
    @Nullable @Override public RsProcMacroStubInfo getProcMacroInfo() { return procMacroInfo; }

    public boolean getMayHaveRustcBuiltinMacro() { return BitUtil.isSet(flags, Macro2StubAttrFlags.MAY_HAVE_RUSTC_BUILTIN_MACRO); }


    public static final RsStubElementType<RsMacro2Stub, RsMacro2> Type =
        new RsStubElementType<RsMacro2Stub, RsMacro2>("MACRO_2") {
            @Nonnull @Override public RsMacro2Stub deserialize(@Nonnull StubInputStream ds, StubElement p) throws IOException {
                return new RsMacro2Stub(p, this,
                    StubImplementationsKt.readNameAsString(ds),
                    ds.readUTFFast(),
                    HashCode.readHashCode(ds),
                    IoUtil.readEnum(ds, MacroBraces.class),
                    ds.readUnsignedByte(),
                    RsProcMacroStubInfo.deserialize(ds));
            }
            @Override public void serialize(@Nonnull RsMacro2Stub s, @Nonnull StubOutputStream ds) throws IOException {
                ds.writeName(s.name);
                ds.writeUTFFast(s.macroBody);
                HashCode.writeHashCode(ds, s.bodyHash);
                IoUtil.writeEnum(ds, s.preferredBraces);
                ds.writeByte(s.flags);
                RsProcMacroStubInfo.serialize(s.procMacroInfo, ds);
            }
            @Nonnull @Override public RsMacro2 createPsi(@Nonnull RsMacro2Stub s) { return new RsMacro2Impl(s, this); }
            @Nonnull @Override public RsMacro2Stub createStub(@Nonnull RsMacro2 psi, @Nullable StubElement p) {
                int flags = RsAttributeOwnerStub.extractFlags(psi, new Macro2StubAttrFlags());
                MacroBraces preferredBraces = psi.getPreferredBraces();
                String body = RsMacro2Util.prepareMacroBody(psi);
                RsProcMacroStubInfo procMacroInfo = RsAttrProcMacroOwnerStub.extractTextAndOffset(flags, psi);
                return new RsMacro2Stub(p, this, psi.getName(), body, HashCode.compute(body), preferredBraces, flags, procMacroInfo);
            }
            @Override public void indexStub(@Nonnull RsMacro2Stub s, @Nonnull IndexSink sink) { StubIndexing.indexMacroDef(sink, s); }
        };
}
