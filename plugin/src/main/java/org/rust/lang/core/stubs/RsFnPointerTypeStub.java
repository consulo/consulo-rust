/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */
package org.rust.lang.core.stubs;

import consulo.language.ast.ASTNode;
import consulo.language.psi.stub.*;
import consulo.util.lang.BitUtil;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
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

    public RsFnPointerTypeStub(@Nullable StubElement parent, @Nonnull IStubElementType elementType,
                                @Nullable String abiName, int flags) {
        super(parent, elementType);
        this.abiName = abiName; this.flags = flags;
    }
    @Nullable public String getAbiName() { return abiName; }
    public boolean isUnsafe() { return BitUtil.isSet(flags, UNSAFE_MASK); }
    public boolean isExtern() { return BitUtil.isSet(flags, EXTERN_MASK); }

    public static final RsStubElementType<RsFnPointerTypeStub, RsFnPointerType> Type =
        new RsStubElementType<RsFnPointerTypeStub, RsFnPointerType>("FN_POINTER_TYPE") {
            @Override public boolean shouldCreateStub(@Nonnull ASTNode node) { return createStubIfParentIsStub(node); }
            @Nonnull @Override public RsFnPointerTypeStub deserialize(@Nonnull StubInputStream ds, StubElement p) throws IOException {
                return new RsFnPointerTypeStub(p, this, StubImplementationsKt.readUTFFastAsNullable(ds), ds.readUnsignedByte());
            }
            @Override public void serialize(@Nonnull RsFnPointerTypeStub s, @Nonnull StubOutputStream ds) throws IOException {
                StubImplementationsKt.writeUTFFastAsNullable(ds, s.abiName); ds.writeByte(s.flags);
            }
            @Nonnull @Override public RsFnPointerType createPsi(@Nonnull RsFnPointerTypeStub s) { return new RsFnPointerTypeImpl(s, this); }
            @Nonnull @Override public RsFnPointerTypeStub createStub(@Nonnull RsFnPointerType psi, @Nullable StubElement p) {
                int flags = 0;
                flags = BitUtil.set(flags, UNSAFE_MASK, RsFnPointerTypeUtil.isUnsafe(psi));
                flags = BitUtil.set(flags, EXTERN_MASK, RsFnPointerTypeUtil.isExtern(psi));
                return new RsFnPointerTypeStub(p, this, RsFnPointerTypeUtil.getAbiName(psi), flags);
            }
        };
}
