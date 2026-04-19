/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.lang.doc.psi.ext;

import com.intellij.psi.PsiElement;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.rust.lang.doc.psi.RsDocComment;

/**
 * Utility class for PSI elements inside doc comments.
 * Delegates to {@link RsDocPsiElementExt} which contains the actual implementations.
 */
public final class DocPsiElementUtil {

    private DocPsiElementUtil() {
    }

    @Nullable
    public static RsDocComment containingDoc(@NotNull PsiElement element) {
        return RsDocPsiElementExt.containingDoc(element);
    }

    public static boolean isInDocComment(@NotNull PsiElement element) {
        return RsDocPsiElementExt.isInDocComment(element);
    }
}
