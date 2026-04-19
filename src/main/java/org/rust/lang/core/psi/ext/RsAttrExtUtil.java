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

/**
 * Extension functions for {@link RsAttr}.
 */
public final class RsAttrExtUtil {

    private RsAttrExtUtil() {
    }

    @Nullable
    public static RsDocAndAttributeOwner getOwner(@NotNull RsAttr attr) {
        if (attr instanceof RsOuterAttr) {
            PsiElement parent = attr.getParent();
            return parent instanceof RsDocAndAttributeOwner ? (RsDocAndAttributeOwner) parent : null;
        }
        if (attr instanceof RsInnerAttr) {
            PsiElement parent = attr.getParent();
            if (parent instanceof RsMembers) {
                PsiElement grandParent = parent.getParent();
                return grandParent instanceof RsDocAndAttributeOwner ? (RsDocAndAttributeOwner) grandParent : null;
            }
            if (parent instanceof RsBlock) {
                PsiElement parentParent = parent.getParent();
                if (parentParent instanceof RsFunction) {
                    return (RsDocAndAttributeOwner) parentParent;
                }
                return parent instanceof RsDocAndAttributeOwner ? (RsDocAndAttributeOwner) parent : null;
            }
            return parent instanceof RsDocAndAttributeOwner ? (RsDocAndAttributeOwner) parent : null;
        }
        throw new IllegalStateException("Unsupported attribute type: " + attr);
    }

    /**
     * Delegates to {@link CfgUtils#isDisabledCfgAttrAttribute(RsAttr, Crate)}.
     */
    public static boolean isDisabledCfgAttrAttribute(@NotNull RsAttr attr, @NotNull Crate crate) {
        return CfgUtils.isDisabledCfgAttrAttribute(attr, crate);
    }
}
