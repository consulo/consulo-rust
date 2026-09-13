/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.lang.core.stubs;

import consulo.language.ast.ASTNode;
import consulo.language.psi.stub.*;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import org.rust.lang.core.psi.ext.RsElement;

import java.io.IOException;
import java.util.function.BiFunction;

public class RsPlaceholderStub<PsiT extends RsElement> extends StubBase<PsiT> {

    public RsPlaceholderStub(@Nullable StubElement parent, @Nonnull IStubElementType elementType) {
        super(parent, elementType);
    }

    public static class Type<PsiT extends RsElement> extends RsStubElementType<RsPlaceholderStub<?>, PsiT> {
        private final BiFunction<RsPlaceholderStub<?>, IStubElementType, PsiT> myPsiCtor;

        public Type(@Nonnull String debugName, @Nonnull BiFunction<RsPlaceholderStub<?>, IStubElementType, PsiT> psiCtor) {
            super(debugName);
            this.myPsiCtor = psiCtor;
        }

        @Override
        public boolean shouldCreateStub(@Nonnull ASTNode node) {
            return createStubIfParentIsStub(node);
        }

        @Nonnull
        @Override
        public RsPlaceholderStub<PsiT> deserialize(@Nonnull StubInputStream dataStream, StubElement parentStub) throws IOException {
            return new RsPlaceholderStub<>(parentStub, this);
        }

        @Override
        public void serialize(@Nonnull RsPlaceholderStub<?> stub, @Nonnull StubOutputStream dataStream) throws IOException {
        }

        @Nonnull
        @Override
        public PsiT createPsi(@Nonnull RsPlaceholderStub<?> stub) {
            return myPsiCtor.apply(stub, this);
        }

        @Nonnull
        @Override
        public RsPlaceholderStub<PsiT> createStub(@Nonnull PsiT psi, @Nullable StubElement parentStub) {
            return new RsPlaceholderStub<>(parentStub, this);
        }
    }
}
