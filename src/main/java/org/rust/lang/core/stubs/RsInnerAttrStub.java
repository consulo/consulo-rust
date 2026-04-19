/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */
package org.rust.lang.core.stubs;

import com.intellij.lang.ASTNode;
import com.intellij.psi.stubs.*;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.rust.lang.core.psi.RsInnerAttr;
import org.rust.lang.core.psi.impl.RsInnerAttrImpl;
import java.io.IOException;

public class RsInnerAttrStub extends StubBase<RsInnerAttr> {

    public RsInnerAttrStub(@Nullable StubElement<?> parent, @NotNull IStubElementType<?, ?> elementType) {
        super(parent, elementType);
    }

    public static final RsStubElementType<RsInnerAttrStub, RsInnerAttr> Type =
        new RsStubElementType<RsInnerAttrStub, RsInnerAttr>("INNER_ATTR") {
            @Override public boolean shouldCreateStub(@NotNull ASTNode node) {
                return StubImplementationsKt.isFunctionBody(node.getTreeParent()) || createStubIfParentIsStub(node);
            }
            @NotNull @Override public RsInnerAttr createPsi(@NotNull RsInnerAttrStub s) { return new RsInnerAttrImpl(s, this); }
            @Override public void serialize(@NotNull RsInnerAttrStub s, @NotNull StubOutputStream ds) throws IOException { }
            @NotNull @Override public RsInnerAttrStub deserialize(@NotNull StubInputStream ds, StubElement p) throws IOException {
                return new RsInnerAttrStub(p, this);
            }
            @NotNull @Override public RsInnerAttrStub createStub(@NotNull RsInnerAttr psi, @Nullable StubElement<?> p) {
                return new RsInnerAttrStub(p, this);
            }
            @Override public void indexStub(@NotNull RsInnerAttrStub s, @NotNull IndexSink sink) {
                StubIndexing.indexInnerAttr(sink, s);
            }
        };
}
