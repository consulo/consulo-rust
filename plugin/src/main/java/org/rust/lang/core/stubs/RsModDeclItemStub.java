/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.lang.core.stubs;

import consulo.language.psi.stub.*;
import consulo.util.lang.BitUtil;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import org.rust.lang.core.psi.RsModDeclItem;
import org.rust.lang.core.psi.impl.RsModDeclItemImpl;

import java.io.IOException;

import static org.rust.lang.core.stubs.RsAttributeOwnerStub.ModStubAttrFlags.MAY_HAVE_MACRO_USE;

public class RsModDeclItemStub extends RsAttributeOwnerStubBase<RsModDeclItem> implements RsNamedStub {
    @Nullable private final String name;
    private final int flags;

    public RsModDeclItemStub(@Nullable StubElement parent, @Nonnull IStubElementType elementType,
                              @Nullable String name, int flags) {
        super(parent, elementType);
        this.name = name;
        this.flags = flags;
    }

    @Nullable @Override public String getName() { return name; }
    @Override protected int getFlags() { return flags; }
    public boolean getMayHaveMacroUse() { return BitUtil.isSet(flags, MAY_HAVE_MACRO_USE); }

    public static final RsStubElementType<RsModDeclItemStub, RsModDeclItem> Type =
        new RsStubElementType<RsModDeclItemStub, RsModDeclItem>("MOD_DECL_ITEM") {
            @Nonnull @Override
            public RsModDeclItemStub deserialize(@Nonnull StubInputStream dataStream, StubElement parentStub) throws IOException {
                return new RsModDeclItemStub(parentStub, this, StubImplementationsKt.readNameAsString(dataStream), dataStream.readUnsignedByte());
            }
            @Override public void serialize(@Nonnull RsModDeclItemStub stub, @Nonnull StubOutputStream dataStream) throws IOException {
                dataStream.writeName(stub.name); dataStream.writeByte(stub.flags);
            }
            @Nonnull @Override public RsModDeclItem createPsi(@Nonnull RsModDeclItemStub stub) { return new RsModDeclItemImpl(stub, this); }
            @Nonnull @Override public RsModDeclItemStub createStub(@Nonnull RsModDeclItem psi, @Nullable StubElement parentStub) {
                return new RsModDeclItemStub(parentStub, this, psi.getName(), RsAttributeOwnerStub.extractFlags(psi, new RsAttributeOwnerStub.ModStubAttrFlags()));
            }
            @Override public void indexStub(@Nonnull RsModDeclItemStub stub, @Nonnull IndexSink sink) { StubIndexing.indexModDeclItem(sink, stub); }
        };
}
