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
import org.rust.lang.core.psi.RsMacro;
import org.rust.lang.core.psi.impl.RsMacroImpl;
import org.rust.lang.core.stubs.RsAttributeOwnerStub.MacroStubAttrFlags;
import org.rust.stdext.HashCode;
import org.rust.stdext.IoUtil;
import java.io.IOException;
import org.rust.lang.core.psi.ext.RsMacroCallUtil;
import org.rust.lang.core.psi.ext.RsMacroUtil;

public class RsMacroStub extends RsAttrProcMacroOwnerStubBase<RsMacro> implements RsNamedStub {
    @Nullable private final String name;
    @Nullable private final String macroBody;
    @Nullable private final HashCode bodyHash;
    @NotNull private final MacroBraces preferredBraces;
    private final int flags;
    @Nullable private final RsProcMacroStubInfo procMacroInfo;

    public RsMacroStub(@Nullable StubElement<?> parent, @NotNull IStubElementType<?, ?> elementType,
                        @Nullable String name, @Nullable String macroBody, @Nullable HashCode bodyHash,
                        @NotNull MacroBraces preferredBraces, int flags, @Nullable RsProcMacroStubInfo procMacroInfo) {
        super(parent, elementType);
        this.name = name; this.macroBody = macroBody; this.bodyHash = bodyHash;
        this.preferredBraces = preferredBraces; this.flags = flags; this.procMacroInfo = procMacroInfo;
    }
    @Nullable @Override public String getName() { return name; }
    @Nullable public String getMacroBody() { return macroBody; }
    @Nullable public HashCode getBodyHash() { return bodyHash; }
    @NotNull public MacroBraces getPreferredBraces() { return preferredBraces; }
    @Override protected int getFlags() { return flags; }
    @Nullable @Override public RsProcMacroStubInfo getProcMacroInfo() { return procMacroInfo; }

    public boolean getMayHaveMacroExport() { return BitUtil.isSet(flags, MacroStubAttrFlags.MAY_HAVE_MACRO_EXPORT); }
    public boolean getMayHaveMacroExportLocalInnerMacros() { return BitUtil.isSet(flags, MacroStubAttrFlags.MAY_HAVE_MACRO_EXPORT_LOCAL_INNER_MACROS); }
    public boolean getMayHaveRustcBuiltinMacro() { return BitUtil.isSet(flags, MacroStubAttrFlags.MAY_HAVE_RUSTC_BUILTIN_MACRO); }

    public static final RsStubElementType<RsMacroStub, RsMacro> Type =
        new RsStubElementType<RsMacroStub, RsMacro>("MACRO") {
            @NotNull @Override public RsMacroStub deserialize(@NotNull StubInputStream ds, StubElement p) throws IOException {
                return new RsMacroStub(p, this,
                    StubImplementationsKt.readNameAsString(ds),
                    StubImplementationsKt.readUTFFastAsNullable(ds),
                    HashCode.readHashCodeNullable(ds),
                    IoUtil.readEnum(ds, MacroBraces.class),
                    ds.readVarInt(),
                    RsProcMacroStubInfo.deserialize(ds));
            }
            @Override public void serialize(@NotNull RsMacroStub s, @NotNull StubOutputStream ds) throws IOException {
                ds.writeName(s.name);
                StubImplementationsKt.writeUTFFastAsNullable(ds, s.macroBody);
                HashCode.writeHashCodeNullable(ds, s.bodyHash);
                IoUtil.writeEnum(ds, s.preferredBraces);
                ds.writeVarInt(s.flags);
                RsProcMacroStubInfo.serialize(s.procMacroInfo, ds);
            }
            @NotNull @Override public RsMacro createPsi(@NotNull RsMacroStub s) { return new RsMacroImpl(s, this); }
            @NotNull @Override public RsMacroStub createStub(@NotNull RsMacro psi, @Nullable StubElement<?> p) {
                int flags = RsAttributeOwnerStub.extractFlags(psi, new MacroStubAttrFlags());
                RsProcMacroStubInfo procMacroInfo = RsAttrProcMacroOwnerStub.extractTextAndOffset(flags, psi);
                return new RsMacroStub(p, this, psi.getName(),
                    RsMacroUtil.getMacroBody(psi) != null ? RsMacroUtil.getMacroBody(psi).getText() : null,
                    psi.getBodyHash(), psi.getPreferredBraces(), flags, procMacroInfo);
            }
            @Override public void indexStub(@NotNull RsMacroStub s, @NotNull IndexSink sink) { StubIndexing.indexMacro(sink, s); }
        };
}
