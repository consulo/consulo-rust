/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.lang.core.psi.ext;

import consulo.language.psi.PsiElement;
import jakarta.annotation.Nonnull;
import org.rust.lang.core.crate.Crate;

/**
 * Bridge class for PsiElement extension methods referenced as RsPsiElementExt.
 */
public final class RsPsiElementExt {

    private RsPsiElementExt() {
    }

    public static boolean isEnabledByCfg(@Nonnull PsiElement element) {
        return CfgUtils.isEnabledByCfg(element);
    }

    public static boolean isEnabledByCfg(@Nonnull PsiElement element, @Nonnull Crate crate) {
        return CfgUtils.isEnabledByCfg(element, crate);
    }
}
