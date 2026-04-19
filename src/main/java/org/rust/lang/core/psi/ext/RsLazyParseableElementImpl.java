/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.lang.core.psi.ext;

import com.intellij.codeInsight.completion.CompletionUtil;
import com.intellij.extapi.psi.ASTWrapperPsiElement;
import com.intellij.lang.ASTNode;
import com.intellij.psi.PsiElement;
import com.intellij.psi.util.PsiTreeUtil;
import org.jetbrains.annotations.NotNull;
import org.rust.lang.core.macros.RsExpandedElementUtil;

/**
 * Base class for lazy-parseable Rust PSI elements (e.g., macro arguments/bodies).
 */
public abstract class RsLazyParseableElementImpl extends ASTWrapperPsiElement implements RsElement {

    public RsLazyParseableElementImpl(@NotNull ASTNode node) {
        super(node);
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
        return getClass().getSimpleName() + "(" + getNode().getElementType() + ")";
    }
}
