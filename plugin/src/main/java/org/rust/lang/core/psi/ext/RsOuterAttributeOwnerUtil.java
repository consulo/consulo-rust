/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.lang.core.psi.ext;

import consulo.language.psi.PsiElement;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import org.rust.lang.core.psi.RsInnerAttr;
import org.rust.lang.core.psi.RsOuterAttr;
import org.rust.lang.core.psi.RsPsiFactory;

/**
 * Bridge class delegating to {@link RsOuterAttributeOwner}.
 */
public final class RsOuterAttributeOwnerUtil {
    private RsOuterAttributeOwnerUtil() {
    }

    /**
     * Adds an outer attribute before the given anchor element.
     */
    public static void addOuterAttribute(
        @Nonnull RsOuterAttributeOwner owner,
        @Nonnull Attribute attr,
        @Nullable PsiElement anchor
    ) {
        RsPsiFactory factory = new RsPsiFactory(((PsiElement) owner).getProject());
        RsOuterAttr outerAttr = factory.createOuterAttr(attr.getText());
        if (anchor != null) {
            ((PsiElement) owner).addBefore(outerAttr, anchor);
        } else {
            ((PsiElement) owner).addBefore(outerAttr, ((PsiElement) owner).getFirstChild());
        }
    }

    /**
     * Adds an inner attribute before the given anchor element.
     */
    public static void addInnerAttribute(
        @Nonnull RsInnerAttributeOwner owner,
        @Nonnull Attribute attr,
        @Nullable PsiElement anchor
    ) {
        RsPsiFactory factory = new RsPsiFactory(((PsiElement) owner).getProject());
        RsInnerAttr innerAttr = factory.createInnerAttr(attr.getText());
        if (anchor != null) {
            ((PsiElement) owner).addBefore(innerAttr, anchor);
        } else {
            ((PsiElement) owner).addBefore(innerAttr, ((PsiElement) owner).getFirstChild());
        }
    }
}
