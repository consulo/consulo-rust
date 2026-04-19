/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.lang.core.psi.ext;

import com.intellij.psi.PsiElement;
import org.jetbrains.annotations.NotNull;

/**
 * Bridge class delegating to RsPsiElementExtUtil.
 */
public final class RsPsiElementUtil {
    private RsPsiElementUtil() {
    }

    public static int getEndOffsetInParent(@NotNull PsiElement element) {
        return RsPsiElementExtUtil.getEndOffsetInParent(element);
    }
}
