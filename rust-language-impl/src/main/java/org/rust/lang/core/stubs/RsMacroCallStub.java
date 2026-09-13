/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */
package org.rust.lang.core.stubs;


import consulo.language.ast.ASTNode;
import consulo.language.psi.stub.*;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import org.rust.lang.core.psi.RsMacroCall;
import org.rust.stdext.HashCode;
import java.io.IOException;

import static org.rust.lang.core.psi.RsElementTypes.*;
import static org.rust.lang.core.psi.impl.RsTokenSets.RS_MOD_OR_FILE;
import org.rust.lang.core.psi.ext.impl.RsMacroCallUtil;
import org.rust.lang.core.psi.impl.RsMacroCallImpl;

public class RsMacroCallStub extends RsAttrProcMacroOwnerStubBase<RsMacroCall> {
    @Nullable public final String macroBody;
    @Nullable public final HashCode bodyHash;
    public final int bodyStartOffset;
    public final int flags;
    @Nullable public final RsProcMacroStubInfo procMacroInfo;

    public RsMacroCallStub(@Nullable StubElement parent, @Nonnull IStubElementType elementType,
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

    @Nonnull
    public RsPathStub getPath() {
        //noinspection ConstantConditions - guaranteed to be non-null by the grammar
        return (RsPathStub) findChildStubByType(RsPathStub.Type);
    }


    public static final RsStubElementType<RsMacroCallStub, RsMacroCall> Type =
        new RsStubElementType<RsMacroCallStub, RsMacroCall>("MACRO_CALL") {
            @Override public boolean shouldCreateStub(@Nonnull ASTNode node) {
                var parent = node.getTreeParent().getElementType();
                return RS_MOD_OR_FILE.contains(parent) || parent == MEMBERS ||
                    (parent == MACRO_EXPR || parent == MACRO_TYPE || parent == BLOCK) && createStubIfParentIsStub(node);
            }
            @Nonnull @Override public RsMacroCallStub deserialize(@Nonnull StubInputStream ds, StubElement p) throws IOException {
                return new RsMacroCallStub(p, this,
                    StubImplementationsKt.readUTFFastAsNullable(ds),
                    HashCode.readHashCodeNullable(ds),
                    ds.readVarInt(),
                    ds.readUnsignedByte(),
                    RsProcMacroStubInfo.deserialize(ds));
            }
            @Override public void serialize(@Nonnull RsMacroCallStub s, @Nonnull StubOutputStream ds) throws IOException {
                StubImplementationsKt.writeUTFFastAsNullable(ds, s.macroBody);
                HashCode.writeHashCodeNullable(ds, s.bodyHash);
                ds.writeVarInt(s.bodyStartOffset);
                ds.writeByte(s.flags);
                RsProcMacroStubInfo.serialize(s.procMacroInfo, ds);
            }
            @Nonnull @Override public RsMacroCall createPsi(@Nonnull RsMacroCallStub s) { return new RsMacroCallImpl(s, this); }
            @Nonnull @Override public RsMacroCallStub createStub(@Nonnull RsMacroCall psi, @Nullable StubElement p) {
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
