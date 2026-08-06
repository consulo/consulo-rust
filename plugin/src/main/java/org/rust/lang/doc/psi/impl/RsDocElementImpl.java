/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.lang.doc.psi.impl;

import consulo.language.editor.completion.CompletionUtilCore;
import consulo.language.impl.ast.AstBufferUtil;
import consulo.language.impl.psi.CompositePsiElement;
import consulo.language.ast.IElementType;
import consulo.language.psi.util.PsiTreeUtil;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import org.rust.lang.core.psi.ext.RsMod;
import org.rust.lang.core.psi.ext.RsPsiJavaUtil;
import org.rust.lang.doc.psi.RsDocComment;
import org.rust.lang.doc.psi.RsDocElement;

public abstract class RsDocElementImpl extends CompositePsiElement implements RsDocElement {

    public RsDocElementImpl(@Nonnull IElementType type) {
        super(type);
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

    @Nonnull
    protected <T> T notNullChild(@Nullable T child) {
        if (child == null) {
            throw new IllegalStateException(getText() + " parent=" + getParent().getText());
        }
        return child;
    }

    @Override
    @Nonnull
    public RsDocComment getContainingDoc() {
        RsDocComment doc = RsPsiJavaUtil.ancestorStrict(this, RsDocComment.class);
        if (doc == null) {
            throw new IllegalStateException("RsDocElement cannot leave outside of the doc comment! `" + getText() + "`");
        }
        return doc;
    }

    @Override
    @Nonnull
    public String getMarkdownValue() {
        return AstBufferUtil.getTextSkippingWhitespaceComments(this);
    }

    @Override
    @Nonnull
    public String toString() {
        return getClass().getSimpleName() + "(" + getNode().getElementType() + ")";
    }
}
