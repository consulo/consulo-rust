/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.lang.core.psi.ext.impl;

import consulo.language.psi.PsiElement;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import org.rust.lang.core.crate.Crate;
import org.rust.lang.core.psi.*;
import org.rust.lang.core.psi.ext.*;

public final class RsCfgExtUtil {
    private RsCfgExtUtil() {}

    public static boolean existsAfterExpansion(@Nonnull PsiElement element, @Nullable Crate crate) {
        return RsElementUtil.existsAfterExpansion(element, crate);
    }

    public static boolean existsAfterExpansionSelf(@Nonnull RsDocAndAttributeOwner self, @Nullable Crate crate) {
        return RsElementUtil.existsAfterExpansionSelf(self, crate);
    }

    public static boolean isCfgUnknown(@Nonnull PsiElement element) {
        return CfgUtils.isCfgUnknown(element);
    }
}
