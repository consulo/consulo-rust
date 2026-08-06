/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.lang.core.psi.ext;

import consulo.language.editor.completion.CompletionUtilCore;
import consulo.language.impl.psi.stub.StubBasedPsiElementBase;
import consulo.language.ast.ASTNode;
import consulo.language.psi.PsiElement;
import consulo.language.psi.stub.IStubElementType;
import consulo.language.psi.stub.StubElement;
import consulo.language.psi.util.PsiTreeUtil;
import jakarta.annotation.Nonnull;
import org.rust.lang.core.macros.RsExpandedElementUtil;

/**
 * Base class for stub-based Rust PSI elements.
 * Generated PSI classes that have stubs extend this class.
 */
@SuppressWarnings({"rawtypes", "unchecked"})
public abstract class RsStubbedElementImpl<StubT extends StubElement> extends StubBasedPsiElementBase<StubT> implements RsElement {

    public RsStubbedElementImpl(@Nonnull ASTNode node) {
        super(node);
    }

    public RsStubbedElementImpl(@Nonnull StubT stub, @Nonnull IStubElementType nodeType) {
        super(stub, nodeType);
    }

    @Nonnull
    @Override
    public RsMod getContainingMod() {
        RsMod mod = PsiTreeUtil.getContextOfType(
            CompletionUtilCore.getOriginalOrSelf(this), RsMod.class, true
        );
        if (mod != null) {
            return CompletionUtilCore.getOriginalOrSelf(mod);
        }
        throw new IllegalStateException("Element outside of module: " + getText());
    }

    @Override
    public PsiElement getNavigationElement() {
        PsiElement target = RsExpandedElementUtil.findNavigationTargetIfMacroExpansion(this);
        return target != null ? target : super.getNavigationElement();
    }

    @Override
    public String toString() {
        return getClass().getSimpleName() + "(" + getElementType() + ")";
    }
}
