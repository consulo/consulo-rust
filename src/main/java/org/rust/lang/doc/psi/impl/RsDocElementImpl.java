/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.lang.doc.psi.impl;

import com.intellij.codeInsight.completion.CompletionUtil;
import com.intellij.psi.impl.source.tree.AstBufferUtil;
import com.intellij.psi.impl.source.tree.CompositePsiElement;
import com.intellij.psi.tree.IElementType;
import com.intellij.psi.util.PsiTreeUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.rust.lang.core.psi.ext.RsMod;
import org.rust.lang.core.psi.ext.RsPsiJavaUtil;
import org.rust.lang.doc.psi.RsDocComment;
import org.rust.lang.doc.psi.RsDocElement;

public abstract class RsDocElementImpl extends CompositePsiElement implements RsDocElement {

    public RsDocElementImpl(@NotNull IElementType type) {
        super(type);
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

    @NotNull
    protected <T> T notNullChild(@Nullable T child) {
        if (child == null) {
            throw new IllegalStateException(getText() + " parent=" + getParent().getText());
        }
        return child;
    }

    @Override
    @NotNull
    public RsDocComment getContainingDoc() {
        RsDocComment doc = RsPsiJavaUtil.ancestorStrict(this, RsDocComment.class);
        if (doc == null) {
            throw new IllegalStateException("RsDocElement cannot leave outside of the doc comment! `" + getText() + "`");
        }
        return doc;
    }

    @Override
    @NotNull
    public String getMarkdownValue() {
        return AstBufferUtil.getTextSkippingWhitespaceComments(this);
    }

    @Override
    @NotNull
    public String toString() {
        return getClass().getSimpleName() + "(" + getNode().getElementType() + ")";
    }
}
