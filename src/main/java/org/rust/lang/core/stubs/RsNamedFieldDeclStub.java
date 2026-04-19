/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */
package org.rust.lang.core.stubs;

import com.intellij.psi.stubs.*;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.rust.lang.core.psi.RsNamedFieldDecl;
import org.rust.lang.core.psi.impl.RsNamedFieldDeclImpl;
import java.io.IOException;

public class RsNamedFieldDeclStub extends RsAttributeOwnerStubBase<RsNamedFieldDecl> implements RsNamedStub {
    @Nullable private final String name;
    private final int flags;

    public RsNamedFieldDeclStub(@Nullable StubElement<?> parent, @NotNull IStubElementType<?, ?> elementType,
                                 @Nullable String name, int flags) {
        super(parent, elementType); this.name = name; this.flags = flags;
    }
    @Nullable @Override public String getName() { return name; }
    @Override protected int getFlags() { return flags; }

    public static final RsStubElementType<RsNamedFieldDeclStub, RsNamedFieldDecl> Type =
        new RsStubElementType<RsNamedFieldDeclStub, RsNamedFieldDecl>("NAMED_FIELD_DECL") {
            @NotNull @Override public RsNamedFieldDeclStub deserialize(@NotNull StubInputStream ds, StubElement p) throws IOException {
                return new RsNamedFieldDeclStub(p, this, StubImplementationsKt.readNameAsString(ds), ds.readUnsignedByte());
            }
            @Override public void serialize(@NotNull RsNamedFieldDeclStub s, @NotNull StubOutputStream ds) throws IOException {
                ds.writeName(s.name); ds.writeByte(s.flags);
            }
            @NotNull @Override public RsNamedFieldDecl createPsi(@NotNull RsNamedFieldDeclStub s) { return new RsNamedFieldDeclImpl(s, this); }
            @NotNull @Override public RsNamedFieldDeclStub createStub(@NotNull RsNamedFieldDecl psi, @Nullable StubElement<?> p) {
                return new RsNamedFieldDeclStub(p, this, psi.getName(), RsAttributeOwnerStub.extractFlags(psi));
            }
            @Override public void indexStub(@NotNull RsNamedFieldDeclStub s, @NotNull IndexSink sink) { StubIndexing.indexNamedFieldDecl(sink, s); }
        };
}
