/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.ide.utils;
import consulo.language.psi.PsiElement;
import consulo.language.psi.PsiFile;
import org.rust.lang.core.psi.RsExpr;

public final class RsBackendUtilUtil {
    private RsBackendUtilUtil() {
    }

    public static org.rust.lang.core.psi.RsExpr findExpressionInRange(
        @org.jetbrains.annotations.NotNull consulo.language.psi.PsiFile file,
        int startOffset,
        int endOffset
    ) {
        return SearchByOffset.findExpressionInRange(file, startOffset, endOffset);
    }

    public static consulo.language.psi.PsiElement[] findStatementsInRange(
        @org.jetbrains.annotations.NotNull consulo.language.psi.PsiFile file,
        int startOffset,
        int endOffset
    ) {
        return SearchByOffset.findStatementsInRange(file, startOffset, endOffset);
    }
}
