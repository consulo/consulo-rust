/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.lang.core.psi.ext;

import com.intellij.psi.PsiElement;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.rust.lang.core.crate.Crate;
import org.rust.lang.core.psi.*;

public final class RsCfgExtUtil {
    private RsCfgExtUtil() {}

    public static boolean existsAfterExpansion(@NotNull PsiElement element, @Nullable Crate crate) {
        return RsElementUtil.existsAfterExpansion(element, crate);
    }

    public static boolean existsAfterExpansionSelf(@NotNull RsDocAndAttributeOwner self, @Nullable Crate crate) {
        return RsElementUtil.existsAfterExpansionSelf(self, crate);
    }

    public static boolean isCfgUnknown(@NotNull PsiElement element) {
        return CfgUtils.isCfgUnknown(element);
    }
}
