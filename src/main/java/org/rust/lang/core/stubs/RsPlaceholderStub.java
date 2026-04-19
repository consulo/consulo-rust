/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.lang.core.stubs;

import com.intellij.lang.ASTNode;
import com.intellij.psi.stubs.*;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.rust.lang.core.psi.ext.RsElement;

import java.io.IOException;
import java.util.function.BiFunction;

public class RsPlaceholderStub<PsiT extends RsElement> extends StubBase<PsiT> {

    public RsPlaceholderStub(@Nullable StubElement<?> parent, @NotNull IStubElementType<?, ?> elementType) {
        super(parent, elementType);
    }

    public static class Type<PsiT extends RsElement> extends RsStubElementType<RsPlaceholderStub<?>, PsiT> {
        private final BiFunction<RsPlaceholderStub<?>, IStubElementType<?, ?>, PsiT> myPsiCtor;

        public Type(@NotNull String debugName, @NotNull BiFunction<RsPlaceholderStub<?>, IStubElementType<?, ?>, PsiT> psiCtor) {
            super(debugName);
            this.myPsiCtor = psiCtor;
        }

        @Override
        public boolean shouldCreateStub(@NotNull ASTNode node) {
            return createStubIfParentIsStub(node);
        }

        @NotNull
        @Override
        public RsPlaceholderStub<PsiT> deserialize(@NotNull StubInputStream dataStream, StubElement parentStub) throws IOException {
            return new RsPlaceholderStub<>(parentStub, this);
        }

        @Override
        public void serialize(@NotNull RsPlaceholderStub<?> stub, @NotNull StubOutputStream dataStream) throws IOException {
        }

        @NotNull
        @Override
        public PsiT createPsi(@NotNull RsPlaceholderStub<?> stub) {
            return myPsiCtor.apply(stub, this);
        }

        @NotNull
        @Override
        public RsPlaceholderStub<PsiT> createStub(@NotNull PsiT psi, @Nullable StubElement<?> parentStub) {
            return new RsPlaceholderStub<>(parentStub, this);
        }
    }
}
