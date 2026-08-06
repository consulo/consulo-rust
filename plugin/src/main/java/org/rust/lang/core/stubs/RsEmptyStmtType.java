/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */
package org.rust.lang.core.stubs;

import consulo.language.ast.ASTNode;
import consulo.language.psi.PsiElement;
import consulo.language.psi.stub.*;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
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
    public boolean shouldCreateStub(@Nonnull ASTNode node) {
        return false;
    }

    @Override
    public void serialize(@Nonnull RsPlaceholderStub<RsEmptyStmt> stub, @Nonnull StubOutputStream dataStream) throws IOException {
        throw new IllegalStateException("EmptyStmtType stub must never be created");
    }

    @Nonnull
    @Override
    public RsPlaceholderStub<RsEmptyStmt> deserialize(@Nonnull StubInputStream dataStream, StubElement parentStub) throws IOException {
        throw new IllegalStateException("EmptyStmtType stub must never be created");
    }

    @Nonnull
    @Override
    public RsPlaceholderStub<RsEmptyStmt> createStub(@Nonnull RsEmptyStmt psi, StubElement parentStub) {
        throw new IllegalStateException("EmptyStmtType stub must never be created");
    }

    @Nonnull
    @Override
    public RsEmptyStmt createPsi(@Nonnull RsPlaceholderStub<RsEmptyStmt> stub) {
        throw new IllegalStateException("EmptyStmtType stub must never be created");
    }
}
