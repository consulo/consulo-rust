/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.lang.core.stubs;


import consulo.language.ast.ASTNode;
import consulo.language.psi.stub.IStubElementType;
import jakarta.annotation.Nonnull;
import org.rust.lang.core.psi.ext.RsElement;

import java.util.function.BiFunction;

public final class RsExprStubType<PsiT extends RsElement> extends RsPlaceholderStub.Type<PsiT> {

    public RsExprStubType(@Nonnull String debugName, @Nonnull BiFunction<RsPlaceholderStub<?>, IStubElementType, PsiT> psiCtor) {
        super(debugName, psiCtor);
    }

    @Override
    public boolean shouldCreateStub(@Nonnull ASTNode node) {
        return StubImplementationsKt.shouldCreateExprStub(node);
    }
}
