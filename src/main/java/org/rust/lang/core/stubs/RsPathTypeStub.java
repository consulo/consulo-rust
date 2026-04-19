/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */
package org.rust.lang.core.stubs;

import com.intellij.lang.ASTNode;
import com.intellij.psi.stubs.*;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.rust.lang.core.psi.RsPathType;
import org.rust.lang.core.psi.impl.RsPathTypeImpl;
import java.io.IOException;

public class RsPathTypeStub extends StubBase<RsPathType> {

    private RsPathTypeStub(@Nullable StubElement<?> parent, @NotNull IStubElementType<?, ?> elementType) {
        super(parent, elementType);
    }

    public static final RsStubElementType<RsPathTypeStub, RsPathType> Type =
        new RsStubElementType<RsPathTypeStub, RsPathType>("PATH_TYPE") {
            @Override public boolean shouldCreateStub(@NotNull ASTNode node) { return createStubIfParentIsStub(node); }
            @NotNull @Override public RsPathTypeStub deserialize(@NotNull StubInputStream ds, StubElement p) throws IOException {
                return new RsPathTypeStub(p, this);
            }
            @Override public void serialize(@NotNull RsPathTypeStub s, @NotNull StubOutputStream ds) throws IOException { }
            @NotNull @Override public RsPathType createPsi(@NotNull RsPathTypeStub s) { return new RsPathTypeImpl(s, this); }
            @NotNull @Override public RsPathTypeStub createStub(@NotNull RsPathType psi, @Nullable StubElement<?> p) {
                return new RsPathTypeStub(p, this);
            }
        };
}
