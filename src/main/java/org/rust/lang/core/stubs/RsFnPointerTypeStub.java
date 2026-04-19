/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */
package org.rust.lang.core.stubs;

import com.intellij.lang.ASTNode;
import com.intellij.psi.stubs.*;
import com.intellij.util.BitUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.rust.lang.core.psi.RsFnPointerType;
import org.rust.lang.core.psi.impl.RsFnPointerTypeImpl;
import org.rust.lang.core.stubs.RsAttributeOwnerStub.CommonStubAttrFlags;
import org.rust.stdext.CollectionsUtil;
import java.io.IOException;
import org.rust.lang.core.psi.ext.RsFnPointerTypeUtil;

public class RsFnPointerTypeStub extends StubBase<RsFnPointerType> {
    @Nullable private final String abiName;
    private final int flags;

    private static final int UNSAFE_MASK;
    private static final int EXTERN_MASK;

    static {
        // BitFlagsBuilder(CommonStubAttrFlags, BYTE) - CommonStubAttrFlags uses 5 bits
        UNSAFE_MASK = CollectionsUtil.makeBitMask(5);
        EXTERN_MASK = CollectionsUtil.makeBitMask(6);
    }

    public RsFnPointerTypeStub(@Nullable StubElement<?> parent, @NotNull IStubElementType<?, ?> elementType,
                                @Nullable String abiName, int flags) {
        super(parent, elementType);
        this.abiName = abiName; this.flags = flags;
    }
    @Nullable public String getAbiName() { return abiName; }
    public boolean isUnsafe() { return BitUtil.isSet(flags, UNSAFE_MASK); }
    public boolean isExtern() { return BitUtil.isSet(flags, EXTERN_MASK); }

    public static final RsStubElementType<RsFnPointerTypeStub, RsFnPointerType> Type =
        new RsStubElementType<RsFnPointerTypeStub, RsFnPointerType>("FN_POINTER_TYPE") {
            @Override public boolean shouldCreateStub(@NotNull ASTNode node) { return createStubIfParentIsStub(node); }
            @NotNull @Override public RsFnPointerTypeStub deserialize(@NotNull StubInputStream ds, StubElement p) throws IOException {
                return new RsFnPointerTypeStub(p, this, StubImplementationsKt.readUTFFastAsNullable(ds), ds.readUnsignedByte());
            }
            @Override public void serialize(@NotNull RsFnPointerTypeStub s, @NotNull StubOutputStream ds) throws IOException {
                StubImplementationsKt.writeUTFFastAsNullable(ds, s.abiName); ds.writeByte(s.flags);
            }
            @NotNull @Override public RsFnPointerType createPsi(@NotNull RsFnPointerTypeStub s) { return new RsFnPointerTypeImpl(s, this); }
            @NotNull @Override public RsFnPointerTypeStub createStub(@NotNull RsFnPointerType psi, @Nullable StubElement<?> p) {
                int flags = 0;
                flags = BitUtil.set(flags, UNSAFE_MASK, RsFnPointerTypeUtil.isUnsafe(psi));
                flags = BitUtil.set(flags, EXTERN_MASK, RsFnPointerTypeUtil.isExtern(psi));
                return new RsFnPointerTypeStub(p, this, RsFnPointerTypeUtil.getAbiName(psi), flags);
            }
        };
}
