/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.lang.core.psi.ext;

import org.rust.lang.core.psi.ext.RsGenericDeclarationUtil;
import com.intellij.psi.search.searches.ReferencesSearch;
import com.intellij.util.Query;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.rust.lang.core.psi.*;
import org.rust.lang.core.resolve.KnownDerivableTrait;
import org.rust.lang.core.resolve.KnownItems;
import org.rust.lang.core.resolve.ref.RsMacroBodyReferenceDelegateImpl;
import org.rust.lang.core.stubs.RsTraitItemStub;
import org.rust.lang.core.types.BoundElement;
import org.rust.lang.core.types.Substitution;
import org.rust.lang.core.types.SubstitutionUtil;
import org.rust.lang.core.types.infer.FoldUtil;
import org.rust.lang.core.types.ty.Ty;
import org.rust.lang.core.types.ty.TyTypeParameter;
import org.rust.openapiext.QueryExt;

import java.util.*;
import java.util.stream.Collectors;
import org.rust.lang.core.psi.ext.RsTraitRefUtil;

/**
 * functions and properties.
 */
public final class RsTraitItemUtil {
    private RsTraitItemUtil() {
    }

    @Nullable
    public static String getLangAttribute(@NotNull RsTraitItem trait) {
        return RsDocAndAttributeOwnerUtil.getQueryAttributes(trait).getLangAttribute();
    }

    public static boolean isSizedTrait(@NotNull RsTraitItem trait) {
        return "sized".equals(getLangAttribute(trait));
    }

    public static boolean isAuto(@NotNull RsTraitItem trait) {
        RsTraitItemStub stub = RsPsiJavaUtil.getGreenStub(trait);
        if (stub != null) return stub.isAuto();
        return trait.getNode().findChildByType(RsElementTypes.AUTO) != null;
    }

    public static boolean isKnownDerivable(@NotNull RsTraitItem trait) {
        String name = trait.getName();
        if (name == null) return false;
        Map<String, KnownDerivableTrait> map = KnownItems.getKNOWN_DERIVABLE_TRAITS();
        KnownDerivableTrait derivableTrait = map.get(name);
        if (derivableTrait == null) return false;
        if (!derivableTrait.shouldUseHardcodedTraitDerive()) return false;
        return trait.equals(derivableTrait.findTrait(KnownItems.getKnownItems(trait)));
    }

    /**
     * Returns the flattened trait hierarchy for the given bound trait element.
     * This traverses supertrait relationships transitively.
     *
     * @param boundTrait the starting bound trait element
     * @param selfTy     optional self type for substitution; may be null
     * @return a collection of all bound trait elements in the hierarchy
     */
    @NotNull
    public static Collection<BoundElement<RsTraitItem>> getFlattenHierarchy(
        @NotNull BoundElement<RsTraitItem> boundTrait,
        @Nullable Ty selfTy
    ) {
        List<BoundElement<RsTraitItem>> result = new ArrayList<>();
        Set<RsTraitItem> visited = new HashSet<>();
        dfs(boundTrait, selfTy, result, visited);
        return result;
    }

    /**
     * Returns the flattened trait hierarchy without self type substitution.
     */
    @NotNull
    public static Collection<BoundElement<RsTraitItem>> getFlattenHierarchy(
        @NotNull BoundElement<RsTraitItem> boundTrait
    ) {
        return getFlattenHierarchy(boundTrait, null);
    }

    @SuppressWarnings("unchecked")
    private static void dfs(
        @NotNull BoundElement<RsTraitItem> boundTrait,
        @Nullable Ty selfTy,
        @NotNull List<BoundElement<RsTraitItem>> result,
        @NotNull Set<RsTraitItem> visited
    ) {
        if (!visited.add(boundTrait.element())) return;
        result.add(boundTrait);

        for (BoundElement<RsTraitItem> rawSuperTrait : getSuperTraits(boundTrait.element())) {
            BoundElement<RsTraitItem> superTrait;
            if (selfTy != null) {
                Map<TyTypeParameter, Ty> selfMap = new HashMap<>();
                selfMap.put(TyTypeParameter.self(), selfTy);
                Substitution selfSubst = SubstitutionUtil.toTypeSubst(selfMap);
                superTrait = FoldUtil.substitute(rawSuperTrait, selfSubst);
            } else {
                superTrait = rawSuperTrait;
            }

            // Infer associated types on supertraits if possible
            BoundElement<RsTraitItem> inferredSuperTrait = superTrait;
            if (!boundTrait.getAssoc().isEmpty()) {
                List<RsTypeAlias> superTraitAssocTypes = RsMembersUtil.getTypes(
                    RsMembersUtil.getExpandedMembers(superTrait.element())
                );
                Map<RsTypeAlias, Ty> inferredAssoc = new LinkedHashMap<>();
                for (Map.Entry<RsTypeAlias, Ty> entry : boundTrait.getAssoc().entrySet()) {
                    if (superTraitAssocTypes.contains(entry.getKey())) {
                        inferredAssoc.put(entry.getKey(), entry.getValue());
                    }
                }
                if (!inferredAssoc.isEmpty()) {
                    inferredAssoc.putAll(superTrait.getAssoc());
                    inferredSuperTrait = new BoundElement<>(
                        superTrait.element(),
                        superTrait.getSubst(),
                        inferredAssoc
                    );
                }
            }

            dfs(
                FoldUtil.substitute(inferredSuperTrait, boundTrait.getSubst()),
                selfTy,
                result,
                visited
            );
        }
    }

    /**
     * Returns all associated types transitively for a bound trait element.
     */
    @NotNull
    public static Collection<RsTypeAlias> getAssociatedTypesTransitively(
        @NotNull BoundElement<RsTraitItem> boundTrait
    ) {
        Collection<BoundElement<RsTraitItem>> hierarchy = getFlattenHierarchy(boundTrait);
        List<RsTypeAlias> result = new ArrayList<>();
        for (BoundElement<RsTraitItem> bt : hierarchy) {
            result.addAll(RsMembersUtil.getTypes(RsMembersUtil.getExpandedMembers(bt.element())));
        }
        return result;
    }

    /**
     * Returns all associated types transitively for a trait item.
     */
    @NotNull
    public static Collection<RsTypeAlias> getAssociatedTypesTransitively(@NotNull RsTraitItem trait) {
        return getAssociatedTypesTransitively(new BoundElement<>(trait));
    }

    /**
     * Finds an associated type by name within the trait and its supertraits.
     */
    @Nullable
    public static RsTypeAlias findAssociatedType(@NotNull RsTraitItem trait, @NotNull String name) {
        Collection<RsTypeAlias> types = getAssociatedTypesTransitively(trait);
        for (RsTypeAlias type : types) {
            if (name.equals(type.getName())) {
                return type;
            }
        }
        return null;
    }

    /**
     * Creates a bound element with the given associated type substituted.
     */
    @NotNull
    public static BoundElement<RsTraitItem> substAssocType(
        @NotNull RsTraitItem trait,
        @NotNull String assocName,
        @Nullable Ty ty
    ) {
        return substAssocType(new BoundElement<>(trait), assocName, ty);
    }

    /**
     * Creates a bound element with the given associated type substituted.
     */
    @NotNull
    public static BoundElement<RsTraitItem> substAssocType(
        @NotNull BoundElement<RsTraitItem> boundTrait,
        @NotNull String assocName,
        @Nullable Ty ty
    ) {
        RsTypeAlias assocType = findAssociatedType(boundTrait.element(), assocName);
        Map<RsTypeAlias, Ty> assoc = boundTrait.getAssoc();
        if (assocType != null && ty != null) {
            assoc = new LinkedHashMap<>(assoc);
            assoc.put(assocType, ty);
        }
        return new BoundElement<>(boundTrait.element(), boundTrait.getSubst(), assoc);
    }

    /**
     * Searches for all implementations of this trait.
     */
    @NotNull
    @SuppressWarnings({"UnstableApiUsage", "unchecked"})
    public static Query<RsImplItem> searchForImplementations(@NotNull RsTraitItem trait) {
        Query<com.intellij.psi.PsiReference> refs = ReferencesSearch.search(trait, trait.getUseScope());
        // Map references to their grandparent (ImplItem)
        Query<com.intellij.psi.PsiElement> mapped = QueryExt.mapQuery(refs, ref -> {
            com.intellij.psi.PsiElement element = ref.getElement();
            com.intellij.psi.PsiElement parent = element.getParent();
            return parent != null ? parent.getParent() : null;
        });
        // Filter to RsImplItem instances that have a type reference
        return QueryExt.filterQuery(
            QueryExt.filterIsInstanceQuery(mapped, RsImplItem.class),
            impl -> impl.getTypeReference() != null
        );
    }

    /**
     * Returns the super traits of the given trait item.
     */
    @NotNull
    private static List<BoundElement<RsTraitItem>> getSuperTraits(@NotNull RsTraitItem trait) {
        List<BoundElement<RsTraitItem>> result = new ArrayList<>();

        // trait Foo where Self: Bar {}
        List<RsWherePred> wherePreds = RsGenericDeclarationUtil.getWherePreds(trait);
        List<RsPolybound> whereBounds = new ArrayList<>();
        for (RsWherePred pred : wherePreds) {
            RsTypeReference typeRef = pred.getTypeReference();
            if (typeRef != null) {
                RsTypeReference skipped = RsTypeReferenceExtUtil.skipParens(typeRef);
                if (skipped instanceof RsPathType) {
                    RsPath path = ((RsPathType) skipped).getPath();
                    if (RsPathUtil.getHasCself(path)) {
                        RsTypeParamBounds bounds = pred.getTypeParamBounds();
                        if (bounds != null) {
                            whereBounds.addAll(bounds.getPolyboundList());
                        }
                    }
                }
            }
        }

        // trait Foo: Bar {}
        List<RsPolybound> allBounds = new ArrayList<>();
        RsTypeParamBounds typeParamBounds = trait.getTypeParamBounds();
        if (typeParamBounds != null) {
            allBounds.addAll(typeParamBounds.getPolyboundList());
        }
        allBounds.addAll(whereBounds);

        for (RsPolybound bound : allBounds) {
            if (RsPolyboundUtil.getHasQ(bound)) continue; // ignore ?Sized
            RsTraitRef traitRef = bound.getBound().getTraitRef();
            if (traitRef != null) {
                BoundElement<RsTraitItem> resolved = RsTraitRefUtil.resolveToBoundTrait(traitRef);
                if (resolved != null) {
                    result.add(resolved);
                }
            }
        }

        return result;
    }
}
