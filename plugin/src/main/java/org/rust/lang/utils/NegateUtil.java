/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.lang.utils;

public final class NegateUtil {
    private NegateUtil() {
    }

    public static consulo.language.psi.PsiElement negate(org.rust.lang.core.psi.RsBinaryExpr expr) {
        return RsBooleanExpUtils.negate(expr);
    }

    public static consulo.language.psi.PsiElement negate(consulo.language.psi.PsiElement element) {
        return RsBooleanExpUtils.negate(element);
    }
}
