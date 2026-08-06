/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.lang.core.psi.ext;

import consulo.language.editor.completion.CompletionUtilCore;
import consulo.language.impl.psi.ASTWrapperPsiElement;
import consulo.language.ast.ASTNode;
import consulo.language.psi.PsiElement;
import consulo.language.psi.util.PsiTreeUtil;
import jakarta.annotation.Nonnull;
import org.rust.lang.core.macros.RsExpandedElementUtil;

/**
 * Base class for lazy-parseable Rust PSI elements (e.g., macro arguments/bodies).
 */
public abstract class RsLazyParseableElementImpl extends ASTWrapperPsiElement implements RsElement {

    public RsLazyParseableElementImpl(@Nonnull ASTNode node) {
        super(node);
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
        return getClass().getSimpleName() + "(" + getNode().getElementType() + ")";
    }
}
