/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.lang.doc.psi.ext;

import com.intellij.psi.PsiElement;
import com.intellij.psi.util.PsiUtilCore;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.rust.lang.core.psi.RsTokenType;
import org.rust.lang.core.psi.ext.RsPsiJavaUtil;
import org.rust.lang.doc.psi.RsDocComment;

public final class RsDocPsiElementUtil {

    private RsDocPsiElementUtil() {
    }

    @Nullable
    public static RsDocComment containingDoc(@NotNull PsiElement element) {
        return RsPsiJavaUtil.ancestorOrSelf(element, RsDocComment.class);
    }

    public static boolean isInDocComment(@NotNull PsiElement element) {
        RsDocComment doc = containingDoc(element);
        if (doc == null) return false;
        return RsTokenType.RS_DOC_COMMENTS.contains(PsiUtilCore.getElementType(doc));
    }
}
