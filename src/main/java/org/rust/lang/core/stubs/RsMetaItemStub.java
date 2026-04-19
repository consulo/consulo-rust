/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */
package org.rust.lang.core.stubs;

import com.intellij.lang.ASTNode;
import com.intellij.psi.stubs.*;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.rust.lang.core.psi.RsMetaItem;
import org.rust.lang.core.psi.impl.RsMetaItemImpl;
import org.rust.lang.core.stubs.common.RsMetaItemPsiOrStub;
import java.io.IOException;

public class RsMetaItemStub extends StubBase<RsMetaItem> implements RsMetaItemPsiOrStub {
    private final boolean hasEq;

    public RsMetaItemStub(@Nullable StubElement<?> parent, @NotNull IStubElementType<?, ?> elementType, boolean hasEq) {
        super(parent, elementType); this.hasEq = hasEq;
    }

    @Override public boolean getHasEq() { return hasEq; }

    @Nullable @Override
    public RsPathStub getPath() {
        return (RsPathStub) findChildStubByType(RsPathStub.Type);
    }

    @Nullable @Override
    public String getValue() {
        RsLitExprStub litStub = (RsLitExprStub) findChildStubByType(RsLitExprStub.Type);
        if (litStub != null && litStub.getKind() instanceof RsStubLiteralKind.StringLiteral) {
            return ((RsStubLiteralKind.StringLiteral) litStub.getKind()).getValue();
        }
        return null;
    }

    @Nullable @Override
    public RsMetaItemArgsStub getMetaItemArgs() {
        return (RsMetaItemArgsStub) findChildStubByType(RsMetaItemArgsStub.Type);
    }

    public static final RsStubElementType<RsMetaItemStub, RsMetaItem> Type =
        new RsStubElementType<RsMetaItemStub, RsMetaItem>("META_ITEM") {
            @Override public boolean shouldCreateStub(@NotNull ASTNode node) { return createStubIfParentIsStub(node); }
            @NotNull @Override public RsMetaItemStub createStub(@NotNull RsMetaItem psi, @Nullable StubElement<?> p) {
                return new RsMetaItemStub(p, this, psi.getEq() != null);
            }
            @NotNull @Override public RsMetaItem createPsi(@NotNull RsMetaItemStub s) { return new RsMetaItemImpl(s, this); }
            @Override public void serialize(@NotNull RsMetaItemStub s, @NotNull StubOutputStream ds) throws IOException {
                ds.writeBoolean(s.hasEq);
            }
            @NotNull @Override public RsMetaItemStub deserialize(@NotNull StubInputStream ds, StubElement p) throws IOException {
                return new RsMetaItemStub(p, this, ds.readBoolean());
            }
            @Override public void indexStub(@NotNull RsMetaItemStub s, @NotNull IndexSink sink) {
                StubIndexing.indexMetaItem(sink, s);
            }
        };
}
