/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.lang.core.stubs;

import consulo.language.psi.stub.*;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import org.rust.lang.core.psi.*;

import java.io.IOException;
import org.rust.lang.core.psi.impl.*;
import org.rust.lang.core.psi.impl.RsEnumVariantImpl;

public class RsEnumVariantStub extends RsAttributeOwnerStubBase<RsEnumVariant> implements RsNamedStub {
    @Nullable private final String name;
    private final int flags;

    public RsEnumVariantStub(@Nullable StubElement parent, @Nonnull IStubElementType elementType,
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
            @Nonnull @Override
            public RsEnumVariantStub deserialize(@Nonnull StubInputStream dataStream, StubElement parentStub) throws IOException {
                return new RsEnumVariantStub(parentStub, this, StubImplementationsKt.readNameAsString(dataStream), dataStream.readUnsignedByte());
            }
            @Override public void serialize(@Nonnull RsEnumVariantStub stub, @Nonnull StubOutputStream dataStream) throws IOException {
                dataStream.writeName(stub.name); dataStream.writeByte(stub.flags);
            }
            @Nonnull @Override public RsEnumVariant createPsi(@Nonnull RsEnumVariantStub stub) { return new RsEnumVariantImpl(stub, this); }
            @Nonnull @Override public RsEnumVariantStub createStub(@Nonnull RsEnumVariant psi, @Nullable StubElement parentStub) {
                return new RsEnumVariantStub(parentStub, this, psi.getName(), RsAttributeOwnerStub.extractFlags(psi));
            }
            @Override public void indexStub(@Nonnull RsEnumVariantStub stub, @Nonnull IndexSink sink) { StubIndexing.indexEnumVariant(sink, stub); }
        };
}
