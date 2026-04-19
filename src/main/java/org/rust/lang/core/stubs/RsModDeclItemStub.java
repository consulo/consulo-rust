/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.lang.core.stubs;

import com.intellij.psi.stubs.*;
import com.intellij.util.BitUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.rust.lang.core.psi.RsModDeclItem;
import org.rust.lang.core.psi.impl.RsModDeclItemImpl;

import java.io.IOException;

import static org.rust.lang.core.stubs.RsAttributeOwnerStub.ModStubAttrFlags.MAY_HAVE_MACRO_USE;

public class RsModDeclItemStub extends RsAttributeOwnerStubBase<RsModDeclItem> implements RsNamedStub {
    @Nullable private final String name;
    private final int flags;

    public RsModDeclItemStub(@Nullable StubElement<?> parent, @NotNull IStubElementType<?, ?> elementType,
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
            @NotNull @Override
            public RsModDeclItemStub deserialize(@NotNull StubInputStream dataStream, StubElement parentStub) throws IOException {
                return new RsModDeclItemStub(parentStub, this, StubImplementationsKt.readNameAsString(dataStream), dataStream.readUnsignedByte());
            }
            @Override public void serialize(@NotNull RsModDeclItemStub stub, @NotNull StubOutputStream dataStream) throws IOException {
                dataStream.writeName(stub.name); dataStream.writeByte(stub.flags);
            }
            @NotNull @Override public RsModDeclItem createPsi(@NotNull RsModDeclItemStub stub) { return new RsModDeclItemImpl(stub, this); }
            @NotNull @Override public RsModDeclItemStub createStub(@NotNull RsModDeclItem psi, @Nullable StubElement<?> parentStub) {
                return new RsModDeclItemStub(parentStub, this, psi.getName(), RsAttributeOwnerStub.extractFlags(psi, new RsAttributeOwnerStub.ModStubAttrFlags()));
            }
            @Override public void indexStub(@NotNull RsModDeclItemStub stub, @NotNull IndexSink sink) { StubIndexing.indexModDeclItem(sink, stub); }
        };
}
