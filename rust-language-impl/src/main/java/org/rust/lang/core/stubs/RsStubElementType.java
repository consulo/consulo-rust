/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.lang.core.stubs;

import consulo.language.ast.ASTNode;
import consulo.language.psi.stub.IStubElementType;
import consulo.language.psi.stub.IndexSink;
import consulo.language.psi.stub.StubElement;
import consulo.language.psi.stub.IStubFileElementType;
import jakarta.annotation.Nonnull;
import org.rust.lang.RsLanguage;
import org.rust.lang.core.psi.ext.RsElement;

public abstract class RsStubElementType<StubT extends StubElement, PsiT extends RsElement>
    extends IStubElementType<StubT, PsiT> {

    protected RsStubElementType(@Nonnull String debugName) {
        super(debugName, RsLanguage.INSTANCE);
    }

    @Nonnull
    @Override
    public final String getExternalId() {
        return "rust." + super.toString();
    }

    @Override
    public void indexStub(@Nonnull StubT stub, @Nonnull IndexSink sink) {
    }

    public static boolean createStubIfParentIsStub(@Nonnull ASTNode node) {
        ASTNode parent = node.getTreeParent();
        if (parent == null) return false;
        if (parent.getElementType() instanceof IStubElementType) {
            return ((IStubElementType) parent.getElementType()).shouldCreateStub(parent);
        }
        return parent.getElementType() instanceof IStubFileElementType<?>;
    }
}
