/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.ide.inspections.lints;

import com.intellij.openapi.util.Key;
import com.intellij.psi.PsiElement;
import com.intellij.psi.util.CachedValue;
import com.intellij.psi.util.CachedValueProvider;
import com.intellij.psi.util.CachedValuesManager;
import com.intellij.psi.util.PsiModificationTracker;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.rust.lang.core.crate.Crate;
import org.rust.lang.core.macros.decl.DeclMacroConstantsUtil;
import org.rust.lang.core.psi.*;
import org.rust.lang.core.psi.ext.*;
import org.rust.lang.core.types.InferenceUtil;
import org.rust.lang.core.types.infer.ResolvedPath;
import org.rust.lang.core.types.InferenceExtensionsUtil;
import org.rust.openapiext.TreeStatus;
import org.rust.openapiext.ProcessElementsWithMacrosUtil;

import java.util.*;
import org.rust.lang.core.psi.ext.RsPathUtil;
import org.rust.lang.core.psi.ext.RsPsiJavaUtil;
// import removed
import org.rust.lang.core.types.RsTypesUtil;
import org.rust.lang.core.psi.ext.RsElement;

public final class RsPathUsageAnalysis {

    private static final Key<CachedValue<PathUsageMap>> PATH_USAGE_KEY = Key.create("PATH_USAGE_KEY");
    private static final List<String> IGNORED_USE_PATHS = Arrays.asList("crate", DeclMacroConstantsUtil.MACRO_DOLLAR_CRATE_IDENTIFIER, "self", "super");

    private RsPathUsageAnalysis() {
    }

    @NotNull
    public static PathUsageMap getPathUsage(@NotNull RsItemsOwner owner) {
        return CachedValuesManager.getCachedValue(owner, PATH_USAGE_KEY, () -> {
            PathUsageMap usages = calculatePathUsages(owner);
            return CachedValueProvider.Result.create(usages, PsiModificationTracker.MODIFICATION_COUNT);
        });
    }

    @NotNull
    private static PathUsageMap calculatePathUsages(@NotNull RsItemsOwner owner) {
        PathUsageMapMutable usage = new PathUsageMapMutable();
        Crate crate = RsElementUtil.getContainingCrate(owner);
        if (!CfgUtils.existsAfterExpansion(owner, crate)) return usage;
        handleSubtree(owner, usage, crate);
        return usage;
    }

    private static void handleSubtree(@NotNull PsiElement root, @NotNull PathUsageMapMutable usage, @NotNull Crate crate) {
        ProcessElementsWithMacrosUtil.processElementsWithMacros(root, element -> {
            if (element instanceof RsDocAndAttributeOwner && !RsDocAndAttributeOwnerUtil.existsAfterExpansionSelf((RsDocAndAttributeOwner) element, crate)) {
                return TreeStatus.SKIP_CHILDREN;
            }
            if (element instanceof RsModItem && element != root) {
                return TreeStatus.SKIP_CHILDREN;
            }
            handleElement(element, usage);
            return TreeStatus.VISIT_CHILDREN;
        });
    }

    private static void handleElement(@NotNull PsiElement element, @NotNull PathUsageMapMutable usage) {
        if (element instanceof RsPatIdent) {
            RsPatBinding patBinding = ((RsPatIdent) element).getPatBinding();
            String name = patBinding.getReferenceName();
            List<RsElement> targets = patBinding.getReference().multiResolve();
            // if targets is empty, there is no way to distinguish "unresolved reference" and "usual pat ident"
            if (!targets.isEmpty()) {
                usage.recordPath(name, targets);
            }
        } else if (element instanceof RsPath) {
            RsPath path = (RsPath) element;
            String name = path.getReferenceName();
            if (name == null) return;
            if (RsPathUtil.getQualifier(path) != null || path.getTypeQual() != null) {
                Set<RsTraitItem> requiredTraits = getAssociatedItemRequiredTraits(path);
                if (requiredTraits == null) requiredTraits = Collections.emptySet();
                usage.recordMethod(name, requiredTraits);
            } else {
                RsUseSpeck useSpeck = RsPsiJavaUtil.parentOfType(path, RsUseSpeck.class);
                if (useSpeck == null || isTopLevel(useSpeck)) {
                    if (IGNORED_USE_PATHS.contains(name)) return;
                    List<RsElement> targets = path.getReference() != null
                        ? path.getReference().multiResolve()
                        : Collections.emptyList();
                    usage.recordPath(name, targets);
                }
            }
        } else if (element instanceof RsMethodCall) {
            RsMethodCall methodCall = (RsMethodCall) element;
            Set<RsTraitItem> requiredTraits = getMethodRequiredTraits(methodCall);
            if (requiredTraits == null) requiredTraits = Collections.emptySet();
            usage.recordMethod(methodCall.getReferenceName(), requiredTraits);
        }
    }

    @Nullable
    private static Set<RsTraitItem> getMethodRequiredTraits(@NotNull RsMethodCall call) {
        org.rust.lang.core.types.infer.RsInferenceResult inference = RsTypesUtil.getInference(call);
        if (inference == null) return null;
        List<? extends org.rust.lang.core.resolve.ref.MethodResolveVariant> result = inference.getResolvedMethod(call);
        if (result == null) return null;
        Set<RsTraitItem> traits = new HashSet<>();
        for (org.rust.lang.core.resolve.ref.MethodResolveVariant variant : result) {
            org.rust.lang.core.resolve.TraitImplSource source = variant.getSource();
            org.rust.lang.core.types.BoundElement<RsTraitItem> implementedTrait = source.getImplementedTrait();
            if (implementedTrait != null) {
                traits.add(implementedTrait.getTypedElement());
            }
        }
        return traits;
    }

    @Nullable
    private static Set<RsTraitItem> getAssociatedItemRequiredTraits(@NotNull RsPath path) {
        PsiElement parent = path.getParent();
        if (!(parent instanceof RsPathExpr)) return null;
        org.rust.lang.core.types.infer.RsInferenceResult inference = RsTypesUtil.getInference(path);
        if (inference == null) return null;
        List<ResolvedPath> resolved = inference.getResolvedPath((RsPathExpr) parent);
        if (resolved == null) return null;
        Set<RsTraitItem> traits = new HashSet<>();
        for (ResolvedPath rp : resolved) {
            if (rp instanceof ResolvedPath.AssocItem) {
                org.rust.lang.core.resolve.TraitImplSource source = ((ResolvedPath.AssocItem) rp).getSource();
                org.rust.lang.core.types.BoundElement<RsTraitItem> implementedTrait = source.getImplementedTrait();
                if (implementedTrait != null) {
                    traits.add(implementedTrait.getTypedElement());
                }
            }
        }
        return traits;
    }

    /**
     * We should collect paths only from relative use specks,
     * that is top-level use specks without `::`
     * E.g. we shouldn't collect such paths: `use ::{foo, bar}`
     */
    private static boolean isTopLevel(@NotNull RsUseSpeck useSpeck) {
        boolean current = useSpeck.getPath() != null || useSpeck.getColoncolon() == null;
        if (!current) return false;
        RsUseSpeck parentSpeck = RsPsiJavaUtil.parentOfType(useSpeck, RsUseSpeck.class);
        return parentSpeck == null || isTopLevel(parentSpeck);
    }
}
