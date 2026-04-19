/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.lang.core.stubs;

import com.intellij.lang.ASTNode;
import com.intellij.psi.stubs.IStubElementType;
import com.intellij.psi.stubs.IndexSink;
import com.intellij.psi.stubs.StubElement;
import com.intellij.psi.tree.IStubFileElementType;
import org.jetbrains.annotations.NotNull;
import org.rust.lang.RsLanguage;
import org.rust.lang.core.psi.ext.RsElement;

public abstract class RsStubElementType<StubT extends StubElement<?>, PsiT extends RsElement>
    extends IStubElementType<StubT, PsiT> {

    protected RsStubElementType(@NotNull String debugName) {
        super(debugName, RsLanguage.INSTANCE);
    }

    @NotNull
    @Override
    public final String getExternalId() {
        return "rust." + super.toString();
    }

    @Override
    public void indexStub(@NotNull StubT stub, @NotNull IndexSink sink) {
    }

    public static boolean createStubIfParentIsStub(@NotNull ASTNode node) {
        ASTNode parent = node.getTreeParent();
        if (parent == null) return false;
        if (parent.getElementType() instanceof IStubElementType<?, ?>) {
            return ((IStubElementType<?, ?>) parent.getElementType()).shouldCreateStub(parent);
        }
        return parent.getElementType() instanceof IStubFileElementType<?>;
    }
}
