/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.lang.core.psi.ext;

import com.intellij.codeInsight.completion.CompletionUtil;
import com.intellij.extapi.psi.StubBasedPsiElementBase;
import com.intellij.lang.ASTNode;
import com.intellij.psi.PsiElement;
import com.intellij.psi.stubs.IStubElementType;
import com.intellij.psi.stubs.StubElement;
import com.intellij.psi.util.PsiTreeUtil;
import org.jetbrains.annotations.NotNull;
import org.rust.lang.core.macros.RsExpandedElementUtil;

/**
 * Base class for stub-based Rust PSI elements.
 * Generated PSI classes that have stubs extend this class.
 */
public abstract class RsStubbedElementImpl<StubT extends StubElement<?>> extends StubBasedPsiElementBase<StubT> implements RsElement {

    public RsStubbedElementImpl(@NotNull ASTNode node) {
        super(node);
    }

    public RsStubbedElementImpl(@NotNull StubT stub, @NotNull IStubElementType<?, ?> nodeType) {
        super(stub, nodeType);
    }

    @NotNull
    @Override
    public RsMod getContainingMod() {
        RsMod mod = PsiTreeUtil.getContextOfType(
            CompletionUtil.getOriginalOrSelf(this), RsMod.class, true
        );
        if (mod != null) {
            return CompletionUtil.getOriginalOrSelf(mod);
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
