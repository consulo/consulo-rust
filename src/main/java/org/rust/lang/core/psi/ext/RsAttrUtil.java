/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.lang.core.psi.ext;

import com.intellij.psi.PsiElement;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.rust.lang.core.psi.RsBlock;
import org.rust.lang.core.psi.RsFunction;
import org.rust.lang.core.psi.RsInnerAttr;
import org.rust.lang.core.psi.RsMembers;
import org.rust.lang.core.psi.RsOuterAttr;
import org.rust.lang.core.stubs.common.RsMetaItemPsiOrStub;

/**
 * Delegates to {@link RsAttrUtil}, {@link RsDocAndAttributeOwnerKt}, and {@link RsMetaItemKt}
 * for the actual implementations.
 */
public final class RsAttrUtil {

    private RsAttrUtil() {
    }

    /**
     * Returns the owner of an attribute element.
     */
    @Nullable
    public static RsDocAndAttributeOwner getOwner(@NotNull RsAttr attr) {
        if (attr instanceof RsOuterAttr) {
            PsiElement parent = attr.getParent();
            return parent instanceof RsDocAndAttributeOwner ? (RsDocAndAttributeOwner) parent : null;
        }
        if (attr instanceof RsInnerAttr) {
            PsiElement parent = attr.getParent();
            if (parent instanceof RsMembers) {
                PsiElement pp = parent.getParent();
                return pp instanceof RsDocAndAttributeOwner ? (RsDocAndAttributeOwner) pp : null;
            }
            if (parent instanceof RsBlock) {
                PsiElement pp = parent.getParent();
                if (pp instanceof RsFunction) {
                    return (RsFunction) pp;
                }
                return parent instanceof RsDocAndAttributeOwner ? (RsDocAndAttributeOwner) parent : null;
            }
            return parent instanceof RsDocAndAttributeOwner ? (RsDocAndAttributeOwner) parent : null;
        }
        throw new IllegalStateException("Unsupported attribute type: " + attr);
    }

    /**
     * Finds an outer attribute by name on the given attribute owner.
     */
    @Nullable
    public static RsOuterAttr findOuterAttr(@NotNull RsOuterAttributeOwner item, @NotNull String name) {
        return RsDocAndAttributeOwnerUtil.findOuterAttr(item, name);
    }

    /**
     * Gets the name of a meta item.
     */
    @Nullable
    public static String getName(@Nullable RsMetaItemPsiOrStub metaItem) {
        if (metaItem == null) return null;
        return RsMetaItemUtil.getName(metaItem);
    }
}
