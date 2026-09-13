/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.lang.core.psi.ext.impl;

import consulo.language.psi.PsiElement;
import jakarta.annotation.Nonnull;
import org.rust.lang.core.psi.ext.*;

/**
 * Bridge class delegating to RsPsiElementExtUtil.
 */
public final class RsPsiElementUtil {
    private RsPsiElementUtil() {
    }

    public static int getEndOffsetInParent(@Nonnull PsiElement element) {
        return RsPsiElementExtUtil.getEndOffsetInParent(element);
    }
}
