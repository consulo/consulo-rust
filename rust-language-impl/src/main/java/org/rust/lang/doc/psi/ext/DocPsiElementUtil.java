/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.lang.doc.psi.ext;

import consulo.language.psi.PsiElement;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import org.rust.lang.doc.psi.RsDocComment;

/**
 * Utility class for PSI elements inside doc comments.
 * Delegates to {@link RsDocPsiElementExt} which contains the actual implementations.
 */
public final class DocPsiElementUtil {

    private DocPsiElementUtil() {
    }

    @Nullable
    public static RsDocComment containingDoc(@Nonnull PsiElement element) {
        return RsDocPsiElementExt.containingDoc(element);
    }

    public static boolean isInDocComment(@Nonnull PsiElement element) {
        return RsDocPsiElementExt.isInDocComment(element);
    }
}
