/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */
package org.rust.lang.core.stubs;

import consulo.language.ast.ASTNode;
import consulo.language.psi.stub.*;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import org.rust.lang.core.psi.RsInnerAttr;
import java.io.IOException;
import org.rust.lang.core.psi.impl.RsInnerAttrImpl;

public class RsInnerAttrStub extends StubBase<RsInnerAttr> {

    public RsInnerAttrStub(@Nullable StubElement parent, @Nonnull IStubElementType elementType) {
        super(parent, elementType);
    }


    public static final RsStubElementType<RsInnerAttrStub, RsInnerAttr> Type =
        new RsStubElementType<RsInnerAttrStub, RsInnerAttr>("INNER_ATTR") {
            @Override public boolean shouldCreateStub(@Nonnull ASTNode node) {
                return StubImplementationsKt.isFunctionBody(node.getTreeParent()) || createStubIfParentIsStub(node);
            }
            @Nonnull @Override public RsInnerAttr createPsi(@Nonnull RsInnerAttrStub s) { return new RsInnerAttrImpl(s, this); }
            @Override public void serialize(@Nonnull RsInnerAttrStub s, @Nonnull StubOutputStream ds) throws IOException { }
            @Nonnull @Override public RsInnerAttrStub deserialize(@Nonnull StubInputStream ds, StubElement p) throws IOException {
                return new RsInnerAttrStub(p, this);
            }
            @Nonnull @Override public RsInnerAttrStub createStub(@Nonnull RsInnerAttr psi, @Nullable StubElement p) {
                return new RsInnerAttrStub(p, this);
            }
            @Override public void indexStub(@Nonnull RsInnerAttrStub s, @Nonnull IndexSink sink) {
                StubIndexing.indexInnerAttr(sink, s);
            }
        };
}
