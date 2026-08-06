/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.lang.core.psi.ext;

import consulo.language.psi.PsiElement;
import consulo.language.psi.search.ReferencesSearch;
import consulo.language.psi.util.PsiTreeUtil;
import consulo.application.util.query.Query;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
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

    @Nonnull
    public static Collection<RsTraitItem> getDerivedTraits(@Nonnull RsStructOrEnumItemElement element) {
        List<RsTraitItem> result = new ArrayList<>();
        for (RsMetaItem meta : getDeriveMetaItems(element)) {
            RsTraitItem trait = RsMetaItemUtil.resolveToDerivedTrait(meta);
            if (trait != null) {
                result.add(trait);
            }
        }
        return result;
    }

    @Nonnull
    public static Map<RsTraitItem, RsMetaItem> getDerivedTraitsToMetaItems(@Nonnull RsStructOrEnumItemElement element) {
        Map<RsTraitItem, RsMetaItem> result = new LinkedHashMap<>();
        for (RsMetaItem meta : getDeriveMetaItems(element)) {
            RsTraitItem trait = RsMetaItemUtil.resolveToDerivedTrait(meta);
            if (trait != null) {
                result.put(trait, meta);
            }
        }
        return result;
    }

    @Nonnull
    public static List<RsMetaItem> getDeriveMetaItems(@Nonnull RsStructOrEnumItemElement element) {
        return getDeriveMetaItemsFromAttributes(RsDocAndAttributeOwnerUtil.getQueryAttributes(element));
    }

    @Nonnull
    public static <T extends RsMetaItemPsiOrStub> List<T> getDeriveMetaItemsFromAttributes(@Nonnull QueryAttributes<T> attributes) {
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
    public static PsiElement getFirstKeyword(@Nonnull RsStructOrEnumItemElement element) {
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

    @Nonnull
    public static Ty getDeclaredType(@Nonnull RsStructOrEnumItemElement element) {
        if (element instanceof RsStructItem) {
            return RsPsiTypeImplUtil.declaredType((RsStructItem) element);
        }
        if (element instanceof RsEnumItem) {
            return RsPsiTypeImplUtil.declaredType((RsEnumItem) element);
        }
        throw new IllegalArgumentException("Unexpected element type: " + element.getClass());
    }

    @Nonnull
    public static Query<RsImplItem> searchForImplementations(@Nonnull RsStructOrEnumItemElement element) {
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
