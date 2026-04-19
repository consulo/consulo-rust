/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.lang.core.psi.ext;

import com.intellij.psi.PsiElement;
import com.intellij.psi.search.searches.ReferencesSearch;
import com.intellij.psi.util.PsiTreeUtil;
import com.intellij.util.Query;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.rust.lang.core.psi.*;
import org.rust.lang.core.stubs.common.RsMetaItemPsiOrStub;
import org.rust.lang.core.types.RsPsiTypeImplUtil;
import org.rust.lang.core.types.ty.Ty;
import org.rust.openapiext.QueryUtil;

import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

public final class RsStructOrEnumItemElementUtil {
    private RsStructOrEnumItemElementUtil() {
    }

    @NotNull
    public static Collection<RsTraitItem> getDerivedTraits(@NotNull RsStructOrEnumItemElement element) {
        List<RsTraitItem> result = new ArrayList<>();
        for (RsMetaItem meta : getDeriveMetaItems(element)) {
            RsTraitItem trait = RsMetaItemUtil.resolveToDerivedTrait(meta);
            if (trait != null) {
                result.add(trait);
            }
        }
        return result;
    }

    @NotNull
    public static Map<RsTraitItem, RsMetaItem> getDerivedTraitsToMetaItems(@NotNull RsStructOrEnumItemElement element) {
        Map<RsTraitItem, RsMetaItem> result = new LinkedHashMap<>();
        for (RsMetaItem meta : getDeriveMetaItems(element)) {
            RsTraitItem trait = RsMetaItemUtil.resolveToDerivedTrait(meta);
            if (trait != null) {
                result.put(trait, meta);
            }
        }
        return result;
    }

    @NotNull
    public static List<RsMetaItem> getDeriveMetaItems(@NotNull RsStructOrEnumItemElement element) {
        return getDeriveMetaItemsFromAttributes(RsDocAndAttributeOwnerUtil.getQueryAttributes(element));
    }

    @NotNull
    public static <T extends RsMetaItemPsiOrStub> List<T> getDeriveMetaItemsFromAttributes(@NotNull QueryAttributes<T> attributes) {
        List<T> result = new ArrayList<>();
        for (T attr : attributes.getDeriveAttributes()) {
            if (attr instanceof RsMetaItem) {
                RsMetaItem meta = (RsMetaItem) attr;
                RsMetaItemArgs args = meta.getMetaItemArgs();
                if (args != null) {
                    for (RsMetaItem child : args.getMetaItemList()) {
                        @SuppressWarnings("unchecked")
                        T casted = (T) child;
                        result.add(casted);
                    }
                }
            }
        }
        return result;
    }

    @Nullable
    public static PsiElement getFirstKeyword(@NotNull RsStructOrEnumItemElement element) {
        if (element instanceof RsStructItem) {
            RsStructItem s = (RsStructItem) element;
            RsVis vis = s.getVis();
            return vis != null ? vis : s.getStruct();
        }
        if (element instanceof RsEnumItem) {
            RsEnumItem e = (RsEnumItem) element;
            RsVis vis = e.getVis();
            return vis != null ? vis : e.getEnum();
        }
        return null;
    }

    @NotNull
    public static Ty getDeclaredType(@NotNull RsStructOrEnumItemElement element) {
        if (element instanceof RsStructItem) {
            return RsPsiTypeImplUtil.declaredType((RsStructItem) element);
        }
        if (element instanceof RsEnumItem) {
            return RsPsiTypeImplUtil.declaredType((RsEnumItem) element);
        }
        throw new IllegalArgumentException("Unexpected element type: " + element.getClass());
    }

    @NotNull
    public static Query<RsImplItem> searchForImplementations(@NotNull RsStructOrEnumItemElement element) {
        return QueryUtil.filterIsInstanceQuery(
            QueryUtil.mapQuery(
                ReferencesSearch.search(element, element.getUseScope()),
                ref -> PsiTreeUtil.getTopmostParentOfType(ref.getElement(), RsTypeReference.class) != null
                    ? PsiTreeUtil.getTopmostParentOfType(ref.getElement(), RsTypeReference.class).getParent()
                    : null
            ),
            RsImplItem.class
        );
    }
}
