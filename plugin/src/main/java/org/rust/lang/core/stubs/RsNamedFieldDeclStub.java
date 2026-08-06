/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */
package org.rust.lang.core.stubs;

import consulo.language.psi.stub.*;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import org.rust.lang.core.psi.RsNamedFieldDecl;
import org.rust.lang.core.psi.impl.RsNamedFieldDeclImpl;
import java.io.IOException;

public class RsNamedFieldDeclStub extends RsAttributeOwnerStubBase<RsNamedFieldDecl> implements RsNamedStub {
    @Nullable private final String name;
    private final int flags;

    public RsNamedFieldDeclStub(@Nullable StubElement parent, @Nonnull IStubElementType elementType,
                                 @Nullable String name, int flags) {
        super(parent, elementType); this.name = name; this.flags = flags;
    }
    @Nullable @Override public String getName() { return name; }
    @Override protected int getFlags() { return flags; }

    public static final RsStubElementType<RsNamedFieldDeclStub, RsNamedFieldDecl> Type =
        new RsStubElementType<RsNamedFieldDeclStub, RsNamedFieldDecl>("NAMED_FIELD_DECL") {
            @Nonnull @Override public RsNamedFieldDeclStub deserialize(@Nonnull StubInputStream ds, StubElement p) throws IOException {
                return new RsNamedFieldDeclStub(p, this, StubImplementationsKt.readNameAsString(ds), ds.readUnsignedByte());
            }
            @Override public void serialize(@Nonnull RsNamedFieldDeclStub s, @Nonnull StubOutputStream ds) throws IOException {
                ds.writeName(s.name); ds.writeByte(s.flags);
            }
            @Nonnull @Override public RsNamedFieldDecl createPsi(@Nonnull RsNamedFieldDeclStub s) { return new RsNamedFieldDeclImpl(s, this); }
            @Nonnull @Override public RsNamedFieldDeclStub createStub(@Nonnull RsNamedFieldDecl psi, @Nullable StubElement p) {
                return new RsNamedFieldDeclStub(p, this, psi.getName(), RsAttributeOwnerStub.extractFlags(psi));
            }
            @Override public void indexStub(@Nonnull RsNamedFieldDeclStub s, @Nonnull IndexSink sink) { StubIndexing.indexNamedFieldDecl(sink, s); }
        };
}
