/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */
package org.rust.lang.core.stubs;

import com.intellij.lang.ASTNode;
import com.intellij.psi.stubs.*;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.rust.lang.core.psi.RsMetaItemArgs;
import org.rust.lang.core.psi.impl.RsMetaItemArgsImpl;
import org.rust.lang.core.stubs.common.RsMetaItemArgsPsiOrStub;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class RsMetaItemArgsStub extends StubBase<RsMetaItemArgs> implements RsMetaItemArgsPsiOrStub {

    public RsMetaItemArgsStub(@Nullable StubElement<?> parent, @NotNull IStubElementType<?, ?> elementType) {
        super(parent, elementType);
    }

    @NotNull @Override
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
            @Override public boolean shouldCreateStub(@NotNull ASTNode node) { return createStubIfParentIsStub(node); }
            @NotNull @Override public RsMetaItemArgsStub createStub(@NotNull RsMetaItemArgs psi, @Nullable StubElement<?> p) {
                return new RsMetaItemArgsStub(p, this);
            }
            @NotNull @Override public RsMetaItemArgs createPsi(@NotNull RsMetaItemArgsStub s) { return new RsMetaItemArgsImpl(s, this); }
            @Override public void serialize(@NotNull RsMetaItemArgsStub s, @NotNull StubOutputStream ds) throws IOException { }
            @NotNull @Override public RsMetaItemArgsStub deserialize(@NotNull StubInputStream ds, StubElement p) throws IOException {
                return new RsMetaItemArgsStub(p, this);
            }
        };
}
