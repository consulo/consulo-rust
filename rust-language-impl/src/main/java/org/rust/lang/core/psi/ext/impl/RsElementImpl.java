/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.lang.core.psi.ext.impl;

import consulo.language.ast.ASTNode;
import consulo.language.editor.completion.CompletionUtilCore;
import consulo.language.impl.psi.ASTWrapperPsiElement;
import consulo.language.psi.PsiElement;
import consulo.language.psi.util.PsiTreeUtil;
import jakarta.annotation.Nonnull;
import org.rust.lang.core.macros.RsExpandedElementUtil;
import jakarta.annotation.Nullable;
import org.rust.lang.core.psi.ext.*;

/**
 * Base class for non-stubbed Rust PSI elements.
 * Generated PSI classes that do not have stubs extend this class.
 * Grammar-kit-generated factory instantiates these with (ASTNode), so the base
 * extends ASTWrapperPsiElement rather than CompositePsiElement.
 */
public abstract class RsElementImpl extends ASTWrapperPsiElement implements RsElement {

    public RsElementImpl(@Nonnull ASTNode node) {
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

    @Nullable
    @Override
    public RsMod getCrateRoot() {
        return RsElementUtil.getCrateRoot(this);
    }

    @Nonnull
    @Override
    public org.rust.lang.core.crate.Crate getContainingCrate() {
        return RsElementUtil.getContainingCrate(this);
    }
}
