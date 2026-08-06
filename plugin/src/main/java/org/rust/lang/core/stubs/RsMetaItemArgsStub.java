/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */
package org.rust.lang.core.stubs;

import consulo.language.ast.ASTNode;
import consulo.language.psi.stub.*;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import org.rust.lang.core.psi.RsMetaItemArgs;
import org.rust.lang.core.psi.impl.RsMetaItemArgsImpl;
import org.rust.lang.core.stubs.common.RsMetaItemArgsPsiOrStub;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class RsMetaItemArgsStub extends StubBase<RsMetaItemArgs> implements RsMetaItemArgsPsiOrStub {

    public RsMetaItemArgsStub(@Nullable StubElement parent, @Nonnull IStubElementType elementType) {
        super(parent, elementType);
    }

    @Nonnull @Override
    public List<RsMetaItemStub> getMetaItemList() {
        List<RsMetaItemStub> result = new ArrayList<>();
        for (Object child : getChildrenStubs()) {
            if (child instanceof RsMetaItemStub) {
                result.add((RsMetaItemStub) child);
            }
        }
        return result;
    }

    public static final RsStubElementType<RsMetaItemArgsStub, RsMetaItemArgs> Type =
        new RsStubElementType<RsMetaItemArgsStub, RsMetaItemArgs>("META_ITEM_ARGS") {
            @Override public boolean shouldCreateStub(@Nonnull ASTNode node) { return createStubIfParentIsStub(node); }
            @Nonnull @Override public RsMetaItemArgsStub createStub(@Nonnull RsMetaItemArgs psi, @Nullable StubElement p) {
                return new RsMetaItemArgsStub(p, this);
            }
            @Nonnull @Override public RsMetaItemArgs createPsi(@Nonnull RsMetaItemArgsStub s) { return new RsMetaItemArgsImpl(s, this); }
            @Override public void serialize(@Nonnull RsMetaItemArgsStub s, @Nonnull StubOutputStream ds) throws IOException { }
            @Nonnull @Override public RsMetaItemArgsStub deserialize(@Nonnull StubInputStream ds, StubElement p) throws IOException {
                return new RsMetaItemArgsStub(p, this);
            }
        };
}
