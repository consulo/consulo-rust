/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.lang.core.stubs;

import com.intellij.lang.ASTNode;
import com.intellij.psi.stubs.IStubElementType;
import com.intellij.psi.stubs.StubElement;
import com.intellij.psi.stubs.StubInputStream;
import com.intellij.psi.stubs.StubOutputStream;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.rust.lang.core.psi.ext.RsElement;

import java.io.IOException;
import java.util.function.BiFunction;

public final class RsExprStubType<PsiT extends RsElement> extends RsPlaceholderStub.Type<PsiT> {

    public RsExprStubType(@NotNull String debugName, @NotNull BiFunction<RsPlaceholderStub<?>, IStubElementType<?, ?>, PsiT> psiCtor) {
        super(debugName, psiCtor);
    }

    @Override
    public boolean shouldCreateStub(@NotNull ASTNode node) {
        return StubImplementationsKt.shouldCreateExprStub(node);
    }
}
