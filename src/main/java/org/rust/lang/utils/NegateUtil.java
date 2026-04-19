/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.lang.utils;

public final class NegateUtil {
    private NegateUtil() {
    }

    public static com.intellij.psi.PsiElement negate(org.rust.lang.core.psi.RsBinaryExpr expr) {
        return RsBooleanExpUtils.negate(expr);
    }

    public static com.intellij.psi.PsiElement negate(com.intellij.psi.PsiElement element) {
        return RsBooleanExpUtils.negate(element);
    }
}
