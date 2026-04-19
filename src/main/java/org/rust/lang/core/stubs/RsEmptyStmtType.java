/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */
package org.rust.lang.core.stubs;

import com.intellij.lang.ASTNode;
import com.intellij.psi.PsiElement;
import com.intellij.psi.stubs.*;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.rust.lang.core.psi.RsEmptyStmt;
import java.io.IOException;

/**
 * This is a fake stub type. The actual stub does not exist and can't be created because
 * shouldCreateStub always returns false. This fake stub is needed in order to conform RsStmt signature.
 */
public class RsEmptyStmtType extends RsStubElementType<RsPlaceholderStub<RsEmptyStmt>, RsEmptyStmt> {
    public static final RsEmptyStmtType INSTANCE = new RsEmptyStmtType();

    private RsEmptyStmtType() {
        super("EMPTY_STMT");
    }

    @Override
    public boolean shouldCreateStub(@NotNull ASTNode node) {
        return false;
    }

    @Override
    public void serialize(@NotNull RsPlaceholderStub<RsEmptyStmt> stub, @NotNull StubOutputStream dataStream) throws IOException {
        throw new IllegalStateException("EmptyStmtType stub must never be created");
    }

    @NotNull
    @Override
    public RsPlaceholderStub<RsEmptyStmt> deserialize(@NotNull StubInputStream dataStream, StubElement parentStub) throws IOException {
        throw new IllegalStateException("EmptyStmtType stub must never be created");
    }

    @NotNull
    @Override
    public RsPlaceholderStub<RsEmptyStmt> createStub(@NotNull RsEmptyStmt psi, @Nullable StubElement<? extends PsiElement> parentStub) {
        throw new IllegalStateException("EmptyStmtType stub must never be created");
    }

    @NotNull
    @Override
    public RsEmptyStmt createPsi(@NotNull RsPlaceholderStub<RsEmptyStmt> stub) {
        throw new IllegalStateException("EmptyStmtType stub must never be created");
    }
}
