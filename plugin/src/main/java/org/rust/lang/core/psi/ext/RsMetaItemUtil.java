/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.lang.core.psi.ext;

import consulo.language.util.ProcessingContext;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import org.rust.lang.core.RsPsiPattern;
import org.rust.lang.core.psi.*;
import org.rust.lang.core.stubs.common.RsMetaItemPsiOrStub;
import org.rust.lang.core.stubs.common.RsPathPsiOrStub;

import java.util.ArrayList;
import java.util.List;
import consulo.language.psi.PsiElement;

public final class RsMetaItemUtil {
    private RsMetaItemUtil() {
    }

    /**
     * Returns identifier name if path inside meta item consists only of this identifier.
     * Otherwise, returns {@code null}.
     */
    @Nullable
    public static String getName(@Nonnull RsMetaItemPsiOrStub metaItem) {
        RsPathPsiOrStub path = metaItem.getPath();
        if (path == null) return null;
        if (path.getHasColonColon()) return null;
        return path.getReferenceName();
    }

    @Nullable
    public static String getId(@Nonnull RsMetaItem metaItem) {
        List<String> segments = new ArrayList<>();
        RsPath path = metaItem.getPath();
        while (path != null) {
            String name = path.getReferenceName();
            if (name == null) return null;
            segments.add(0, name);
            path = path.getPath();
        }
        if (segments.isEmpty()) return null;
        return String.join("::", segments);
    }

    /**
     * Works only for known derivable traits.
     */
    @Nullable
    public static RsTraitItem resolveToDerivedTrait(@Nonnull RsMetaItem metaItem) {
        RsPath path = metaItem.getPath();
        if (path == null || path.getReference() == null) return null;
        for (Object resolved : path.getReference().multiResolve()) {
            if (resolved instanceof RsTraitItem) {
                return (RsTraitItem) resolved;
            }
        }
        return null;
    }

    @Nullable
    public static RsDocAndAttributeOwner getOwner(@Nonnull RsMetaItem metaItem) {
        RsAttr attr = RsPsiJavaUtil.ancestorStrict(metaItem, RsAttr.class);
        if (attr == null) return null;
        return RsAttrUtil.getOwner(attr);
    }

    /**
     * Checks whether this meta item is the "root" meta item.
     * In the case of {@code #[foo(bar)]}, the {@code foo(bar)} meta item is "root" but {@code bar} is not.
     * In the case of {@code #[cfg_attr(windows, foo(bar))]}, the {@code foo(bar)} is also "root"
     * because after cfg_attr expanding it will turn into {@code #[foo(bar)]}.
     */
    public static boolean isRootMetaItem(@Nonnull RsMetaItem metaItem) {
        return isRootMetaItem(metaItem, null);
    }

    public static boolean isRootMetaItem(@Nonnull RsMetaItem metaItem, @Nullable ProcessingContext context) {
        consulo.language.psi.PsiElement parent = metaItem.getParent();
        if (parent instanceof RsAttr) {
            if (context != null) {
                context.put(RsPsiPattern.META_ITEM_ATTR, (RsAttr) parent);
            }
            return true;
        }
        return isCfgAttrBody(metaItem, context);
    }

    private static boolean isCfgAttrBody(@Nonnull RsMetaItem metaItem, @Nullable ProcessingContext context) {
        consulo.language.psi.PsiElement parent = metaItem.getParent();
        if (!(parent instanceof RsMetaItemArgs)) return false;
        consulo.language.psi.PsiElement parentMetaItem = parent.getParent();
        if (!(parentMetaItem instanceof RsMetaItem)) return false;
        if (!isCfgAttrMetaItem((RsMetaItem) parentMetaItem, context)) return false;
        List<RsMetaItem> metaItemList = ((RsMetaItemArgs) parent).getMetaItemList();
        RsMetaItem conditionPart = metaItemList.isEmpty() ? null : metaItemList.get(0);
        return metaItem != conditionPart;
    }

    private static boolean isCfgAttrMetaItem(@Nonnull RsMetaItem metaItem, @Nullable ProcessingContext context) {
        return "cfg_attr".equals(getName(metaItem)) && isRootMetaItem(metaItem, context);
    }

    /**
     * Returns true if this meta item is a macro call (proc macro).
     */
    public static boolean isMacroCall(@Nonnull RsMetaItem metaItem) {
        return RsPossibleMacroCallUtil.isMacroCall(metaItem);
    }

    @Nonnull
    public static AttributeTemplateType getTemplateType(@Nonnull RsMetaItem metaItem) {
        if (metaItem.getMetaItemArgs() != null) return AttributeTemplateType.List;
        if (metaItem.getEq() != null) return AttributeTemplateType.NameValueStr;
        return AttributeTemplateType.Word;
    }
}
