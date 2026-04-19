/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.lang.core.psi.ext;

import com.intellij.psi.PsiElement;
import org.jetbrains.annotations.NotNull;
import org.rust.lang.core.crate.Crate;

/**
 * Bridge class for PsiElement extension methods referenced as RsPsiElementExt.
 */
public final class RsPsiElementExt {

    private RsPsiElementExt() {
    }

    public static boolean isEnabledByCfg(@NotNull PsiElement element) {
        return CfgUtils.isEnabledByCfg(element);
    }

    public static boolean isEnabledByCfg(@NotNull PsiElement element, @NotNull Crate crate) {
        return CfgUtils.isEnabledByCfg(element, crate);
    }
}
