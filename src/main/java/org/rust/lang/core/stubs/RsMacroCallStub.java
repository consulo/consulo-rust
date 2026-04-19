/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */
package org.rust.lang.core.stubs;

import com.intellij.lang.ASTNode;
import com.intellij.psi.stubs.*;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.rust.lang.core.psi.RsMacroCall;
import org.rust.lang.core.psi.impl.RsMacroCallImpl;
import org.rust.stdext.HashCode;
import java.io.IOException;

import static org.rust.lang.core.psi.RsElementTypes.*;
import static org.rust.lang.core.psi.RsTokenType.RS_MOD_OR_FILE;
import org.rust.lang.core.psi.ext.RsMacroCallUtil;

public class RsMacroCallStub extends RsAttrProcMacroOwnerStubBase<RsMacroCall> {
    @Nullable private final String macroBody;
    @Nullable private final HashCode bodyHash;
    private final int bodyStartOffset;
    private final int flags;
    @Nullable private final RsProcMacroStubInfo procMacroInfo;

    public RsMacroCallStub(@Nullable StubElement<?> parent, @NotNull IStubElementType<?, ?> elementType,
                            @Nullable String macroBody, @Nullable HashCode bodyHash, int bodyStartOffset,
                            int flags, @Nullable RsProcMacroStubInfo procMacroInfo) {
        super(parent, elementType);
        this.macroBody = macroBody; this.bodyHash = bodyHash; this.bodyStartOffset = bodyStartOffset;
        this.flags = flags; this.procMacroInfo = procMacroInfo;
    }
    @Nullable public String getMacroBody() { return macroBody; }
    @Nullable public HashCode getBodyHash() { return bodyHash; }
    public int getBodyStartOffset() { return bodyStartOffset; }
    @Override protected int getFlags() { return flags; }
    @Nullable @Override public RsProcMacroStubInfo getProcMacroInfo() { return procMacroInfo; }

    @NotNull
    public RsPathStub getPath() {
        //noinspection ConstantConditions - guaranteed to be non-null by the grammar
        return (RsPathStub) findChildStubByType(RsPathStub.Type);
    }

    public static final RsStubElementType<RsMacroCallStub, RsMacroCall> Type =
        new RsStubElementType<RsMacroCallStub, RsMacroCall>("MACRO_CALL") {
            @Override public boolean shouldCreateStub(@NotNull ASTNode node) {
                var parent = node.getTreeParent().getElementType();
                return RS_MOD_OR_FILE.contains(parent) || parent == MEMBERS ||
                    (parent == MACRO_EXPR || parent == MACRO_TYPE || parent == BLOCK) && createStubIfParentIsStub(node);
            }
            @NotNull @Override public RsMacroCallStub deserialize(@NotNull StubInputStream ds, StubElement p) throws IOException {
                return new RsMacroCallStub(p, this,
                    StubImplementationsKt.readUTFFastAsNullable(ds),
                    HashCode.readHashCodeNullable(ds),
                    ds.readVarInt(),
                    ds.readUnsignedByte(),
                    RsProcMacroStubInfo.deserialize(ds));
            }
            @Override public void serialize(@NotNull RsMacroCallStub s, @NotNull StubOutputStream ds) throws IOException {
                StubImplementationsKt.writeUTFFastAsNullable(ds, s.macroBody);
                HashCode.writeHashCodeNullable(ds, s.bodyHash);
                ds.writeVarInt(s.bodyStartOffset);
                ds.writeByte(s.flags);
                RsProcMacroStubInfo.serialize(s.procMacroInfo, ds);
            }
            @NotNull @Override public RsMacroCall createPsi(@NotNull RsMacroCallStub s) { return new RsMacroCallImpl(s, this); }
            @NotNull @Override public RsMacroCallStub createStub(@NotNull RsMacroCall psi, @Nullable StubElement<?> p) {
                int flags = RsAttributeOwnerStub.extractFlags(psi);
                RsProcMacroStubInfo procMacroInfo = RsAttrProcMacroOwnerStub.extractTextAndOffset(flags, psi);
                return new RsMacroCallStub(p, this,
                    RsMacroCallUtil.getMacroBody(psi),
                    RsMacroCallUtil.getBodyHash(psi),
                    RsMacroCallUtil.getBodyTextRange(psi) != null ? RsMacroCallUtil.getBodyTextRange(psi).getStartOffset() : -1,
                    flags, procMacroInfo);
            }
        };
}
