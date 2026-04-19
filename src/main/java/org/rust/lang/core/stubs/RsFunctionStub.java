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
import org.rust.lang.core.psi.RsFunction;
import org.rust.lang.core.psi.ext.RsFunctionUtil;
import org.rust.lang.core.psi.ext.RsMacroDefinitionBaseUtil;
import org.rust.lang.core.psi.impl.RsFunctionImpl;
import org.rust.stdext.BitFlagsBuilder;
import java.io.IOException;

import static org.rust.lang.core.stubs.RsAttributeOwnerStub.FunctionStubAttrFlags.MAY_BE_PROC_MACRO_DEF;
import org.rust.lang.core.psi.ext.RsFunctionUtil;

public class RsFunctionStub extends RsAttrProcMacroOwnerStubBase<RsFunction> implements RsNamedStub {
    @Nullable private final String name;
    @Nullable private final String abiName;
    private final int flags;
    @Nullable private final RsProcMacroStubInfo procMacroInfo;

    private static final int ABSTRACT_MASK;
    private static final int CONST_MASK;
    private static final int UNSAFE_MASK;
    private static final int EXTERN_MASK;
    private static final int VARIADIC_MASK;
    private static final int ASYNC_MASK;
    private static final int HAS_SELF_PARAMETER_MASK;
    private static final int PREFERRED_BRACES;

    static {
        BitFlagsBuilder b = new BitFlagsBuilder(new RsAttributeOwnerStub.FunctionStubAttrFlags(), BitFlagsBuilder.Limit.INT);
        ABSTRACT_MASK = b.nextBitMask();
        CONST_MASK = b.nextBitMask();
        UNSAFE_MASK = b.nextBitMask();
        EXTERN_MASK = b.nextBitMask();
        VARIADIC_MASK = b.nextBitMask();
        ASYNC_MASK = b.nextBitMask();
        HAS_SELF_PARAMETER_MASK = b.nextBitMask();
        int mask = b.nextBitMask();
        b.nextBitMask(); // second bit
        PREFERRED_BRACES = Integer.numberOfTrailingZeros(mask);
    }

    public RsFunctionStub(@Nullable StubElement<?> parent, @NotNull IStubElementType<?, ?> elementType,
                           @Nullable String name, @Nullable String abiName, int flags,
                           @Nullable RsProcMacroStubInfo procMacroInfo) {
        super(parent, elementType);
        this.name = name; this.abiName = abiName; this.flags = flags; this.procMacroInfo = procMacroInfo;
    }

    @Nullable @Override public String getName() { return name; }
    @Nullable public String getAbiName() { return abiName; }
    @Override protected int getFlags() { return flags; }
    @Nullable @Override public RsProcMacroStubInfo getProcMacroInfo() { return procMacroInfo; }

    public boolean isAbstract() { return BitUtil.isSet(flags, ABSTRACT_MASK); }
    public boolean isConst() { return BitUtil.isSet(flags, CONST_MASK); }
    public boolean isUnsafe() { return BitUtil.isSet(flags, UNSAFE_MASK); }
    public boolean isExtern() { return BitUtil.isSet(flags, EXTERN_MASK); }
    public boolean isVariadic() { return BitUtil.isSet(flags, VARIADIC_MASK); }
    public boolean isAsync() { return BitUtil.isSet(flags, ASYNC_MASK); }
    public boolean getHasSelfParameters() { return BitUtil.isSet(flags, HAS_SELF_PARAMETER_MASK); }
    public boolean getMayBeProcMacroDef() { return BitUtil.isSet(flags, MAY_BE_PROC_MACRO_DEF); }
    @NotNull public MacroBraces getPreferredBraces() { return MacroBraces.values()[(flags >>> PREFERRED_BRACES) & 3]; }

    public static final RsStubElementType<RsFunctionStub, RsFunction> Type =
        new RsStubElementType<RsFunctionStub, RsFunction>("FUNCTION") {
            @NotNull @Override
            public RsFunctionStub deserialize(@NotNull StubInputStream dataStream, StubElement parentStub) throws IOException {
                String name = dataStream.readName() != null ? dataStream.readName().getString() : null;
                String abiName = StubImplementationsKt.readUTFFastAsNullable(dataStream);
                int flags = dataStream.readInt();
                RsProcMacroStubInfo info = RsProcMacroStubInfo.deserialize(dataStream);
                return new RsFunctionStub(parentStub, this, name, abiName, flags, info);
            }
            @Override public void serialize(@NotNull RsFunctionStub stub, @NotNull StubOutputStream dataStream) throws IOException {
                dataStream.writeName(stub.name);
                StubImplementationsKt.writeUTFFastAsNullable(dataStream, stub.abiName);
                dataStream.writeInt(stub.flags);
                RsProcMacroStubInfo.serialize(stub.procMacroInfo, dataStream);
            }
            @NotNull @Override public RsFunction createPsi(@NotNull RsFunctionStub stub) { return new RsFunctionImpl(stub, this); }
            @NotNull @Override public RsFunctionStub createStub(@NotNull RsFunction psi, @Nullable StubElement<?> parentStub) {
                int flags = RsAttributeOwnerStub.extractFlags(psi, new RsAttributeOwnerStub.FunctionStubAttrFlags());
                flags = BitUtil.set(flags, ABSTRACT_MASK, RsFunctionUtil.getBlock(psi) == null);
                flags = BitUtil.set(flags, CONST_MASK, RsFunctionUtil.isConst(psi));
                flags = BitUtil.set(flags, UNSAFE_MASK, psi.isUnsafe());
                flags = BitUtil.set(flags, EXTERN_MASK, RsFunctionUtil.isExtern(psi));
                flags = BitUtil.set(flags, VARIADIC_MASK, RsFunctionUtil.isVariadic(psi));
                flags = BitUtil.set(flags, ASYNC_MASK, RsFunctionUtil.isAsync(psi));
                flags = BitUtil.set(flags, HAS_SELF_PARAMETER_MASK, RsFunctionUtil.getHasSelfParameters(psi));
                MacroBraces preferredBraces = BitUtil.isSet(flags, MAY_BE_PROC_MACRO_DEF) ? RsMacroDefinitionBaseUtil.guessPreferredBraces(psi) : MacroBraces.PARENS;
                flags = flags | (preferredBraces.ordinal() << PREFERRED_BRACES);
                RsProcMacroStubInfo procMacroInfo = RsAttrProcMacroOwnerStub.extractTextAndOffset(flags, psi);
                return new RsFunctionStub(parentStub, this, psi.getName(), RsFunctionUtil.getLiteralAbiName(psi), flags, procMacroInfo);
            }
            @Override public void indexStub(@NotNull RsFunctionStub stub, @NotNull IndexSink sink) { StubIndexing.indexFunction(sink, stub); }
        };
}
