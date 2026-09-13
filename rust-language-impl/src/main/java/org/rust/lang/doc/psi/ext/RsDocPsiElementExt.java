/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.lang.doc.psi.ext;

import consulo.language.psi.PsiElement;
import consulo.language.psi.PsiUtilCore;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import org.rust.lang.core.psi.ext.impl.RsPsiJavaUtil;
import org.rust.lang.doc.psi.RsDocComment;
import org.rust.lang.core.psi.impl.RsTokenSets;

public final class RsDocPsiElementExt {

    private RsDocPsiElementExt() {
    }

    @Nullable
    public static RsDocComment containingDoc(@Nonnull PsiElement element) {
        return RsPsiJavaUtil.ancestorOrSelf(element, RsDocComment.class);
    }

    public static boolean isInDocComment(@Nonnull PsiElement element) {
        RsDocComment doc = containingDoc(element);
        if (doc == null) return false;
        return RsTokenSets.RS_DOC_COMMENTS.contains(PsiUtilCore.getElementType(doc));
    }
}
