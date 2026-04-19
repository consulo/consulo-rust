/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.lang.core.stubs;

import com.intellij.psi.stubs.*;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.rust.lang.core.psi.*;
import org.rust.lang.core.psi.impl.RsEnumVariantImpl;

import java.io.IOException;

public class RsEnumVariantStub extends RsAttributeOwnerStubBase<RsEnumVariant> implements RsNamedStub {
    @Nullable private final String name;
    private final int flags;

    public RsEnumVariantStub(@Nullable StubElement<?> parent, @NotNull IStubElementType<?, ?> elementType,
                              @Nullable String name, int flags) {
        super(parent, elementType);
        this.name = name;
        this.flags = flags;
    }

    @Nullable @Override public String getName() { return name; }
    @Override protected int getFlags() { return flags; }

    @SuppressWarnings("unchecked") @Nullable
    public StubElement<RsBlockFields> getBlockFields() { return (StubElement<RsBlockFields>) findChildStubByType(RsStubElementTypes.BLOCK_FIELDS); }

    public static final RsStubElementType<RsEnumVariantStub, RsEnumVariant> Type =
        new RsStubElementType<RsEnumVariantStub, RsEnumVariant>("ENUM_VARIANT") {
            @NotNull @Override
            public RsEnumVariantStub deserialize(@NotNull StubInputStream dataStream, StubElement parentStub) throws IOException {
                return new RsEnumVariantStub(parentStub, this, StubImplementationsKt.readNameAsString(dataStream), dataStream.readUnsignedByte());
            }
            @Override public void serialize(@NotNull RsEnumVariantStub stub, @NotNull StubOutputStream dataStream) throws IOException {
                dataStream.writeName(stub.name); dataStream.writeByte(stub.flags);
            }
            @NotNull @Override public RsEnumVariant createPsi(@NotNull RsEnumVariantStub stub) { return new RsEnumVariantImpl(stub, this); }
            @NotNull @Override public RsEnumVariantStub createStub(@NotNull RsEnumVariant psi, @Nullable StubElement<?> parentStub) {
                return new RsEnumVariantStub(parentStub, this, psi.getName(), RsAttributeOwnerStub.extractFlags(psi));
            }
            @Override public void indexStub(@NotNull RsEnumVariantStub stub, @NotNull IndexSink sink) { StubIndexing.indexEnumVariant(sink, stub); }
        };
}
