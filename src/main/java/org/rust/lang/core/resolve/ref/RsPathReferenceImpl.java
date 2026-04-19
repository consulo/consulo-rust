/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.lang.core.resolve.ref;

import com.intellij.extapi.psi.StubBasedPsiElementBase;
import com.intellij.psi.PsiElement;
import com.intellij.psi.ResolveResult;
import com.intellij.psi.stubs.StubElement;
import com.intellij.psi.tree.IElementType;
import com.intellij.psi.tree.TokenSet;
import com.intellij.util.SmartList;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.VisibleForTesting;
import org.rust.lang.core.psi.*;
import org.rust.lang.core.psi.RsTokenType;
import org.rust.lang.core.psi.ext.*;
import org.rust.lang.core.psi.ext.RsGenericDeclarationUtil;
import org.rust.lang.core.psi.ext.RsPathUtil;
import org.rust.lang.core.resolve.*;
import org.rust.lang.core.stubs.RsPathStub;
import org.rust.lang.core.types.BoundElement;
import org.rust.lang.core.types.RsPsiSubstitution;
import org.rust.lang.core.types.infer.ResolvedPath;
import org.rust.lang.core.types.infer.TyLowering;
import org.rust.lang.core.types.infer.FoldUtil;
import org.rust.lang.core.types.ExtensionsUtil;
import org.rust.lang.core.types.ty.TyProjection;
import org.rust.lang.core.types.ty.TyUnknown;
import org.rust.lang.doc.psi.RsDocPathLinkParent;
import org.rust.lang.utils.evaluation.PathExprResolver;
import org.rust.openapiext.OpenApiUtil;
import org.rust.stdext.StdextUtil;

import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

public class RsPathReferenceImpl extends RsReferenceBase<RsPath> implements RsPathReference {

    public RsPathReferenceImpl(@NotNull RsPath element) {
        super(element);
    }

    @Override
    public boolean isReferenceTo(@NotNull PsiElement target) {
        if (target instanceof RsFieldDecl) return false;

        RsPath path = getElement();
        PsiElement pathParent = path.getParent();

        if (target instanceof RsMod) {
            boolean canReferToMod = pathParent instanceof RsPath || pathParent instanceof RsUseSpeck
                || pathParent instanceof RsVisRestriction || pathParent instanceof RsDocPathLinkParent
                || pathParent instanceof RsPathCodeFragment;
            return canReferToMod && isReferenceToNonAssocItem(target);
        }

        if (target instanceof RsNamedElement) {
            Set<Namespace> targetNs = Namespace.getNamespaces((RsNamedElement) target);
            Set<Namespace> allowedNs = NameResolutionUtil.allowedNamespaces(path);
            if (Collections.disjoint(targetNs, allowedNs)) {
                return false;
            }
        }

        RsAbstractableOwner targetAbstractableOwner = null;
        if (target instanceof RsAbstractable) {
            targetAbstractableOwner = RsAbstractableOwnerUtil.getOwner((RsAbstractable) target);
        }

        if (pathParent instanceof RsPathExpr) {
            if (targetAbstractableOwner != null && targetAbstractableOwner.isImplOrTrait()) {
                if (target instanceof RsTypeAlias) return false;
                if (path.getPath() == null && path.getTypeQual() == null) return false;

                List<ScopeEntry> resolvedRaw = resolvePathRaw(path, null, true);
                com.intellij.psi.PsiManager mgr = target.getManager();

                if (targetAbstractableOwner == RsAbstractableOwner.Free
                    || targetAbstractableOwner == RsAbstractableOwner.Foreign) {
                    return resolvedRaw.stream().anyMatch(it -> mgr.areElementsEquivalent(it.getElement(), target));
                } else if (targetAbstractableOwner instanceof RsAbstractableOwner.Impl) {
                    RsAbstractableOwner.Impl implOwner = (RsAbstractableOwner.Impl) targetAbstractableOwner;
                    if (implOwner.isInherent()) {
                        return resolvedRaw.stream().anyMatch(it -> mgr.areElementsEquivalent(it.getElement(), target));
                    } else {
                        if (resolvedRaw.size() == 1 && mgr.areElementsEquivalent(resolvedRaw.get(0).getElement(), target)) {
                            return true;
                        }
                        RsAbstractable superItem = ((RsAbstractable) target).getSuperItem();
                        if (superItem == null) return false;
                        boolean canBeReferenceTo = resolvedRaw.stream().anyMatch(it ->
                            mgr.areElementsEquivalent(it.getElement(), target) ||
                                mgr.areElementsEquivalent(it.getElement(), superItem)
                        );
                        return canBeReferenceTo && isReferenceToAssocItem(target);
                    }
                } else if (targetAbstractableOwner instanceof RsAbstractableOwner.Trait) {
                    boolean canBeReferenceTo = resolvedRaw.stream().anyMatch(it ->
                        mgr.areElementsEquivalent(it.getElement(), target)
                    );
                    return canBeReferenceTo && isReferenceToAssocItem(target);
                }
                return false;
            } else {
                return isReferenceToNonAssocItem(target);
            }
        } else {
            if (targetAbstractableOwner != null && targetAbstractableOwner.isImplOrTrait()) {
                if (target instanceof RsTypeAlias && pathParent instanceof RsAssocTypeBinding) {
                    return isReferenceToAssocItem(target);
                }
                if (pathParent instanceof RsUseSpeck || (path.getPath() == null && path.getTypeQual() == null)) {
                    return false;
                }
                return isReferenceToAssocItem(target);
            } else {
                return isReferenceToNonAssocItem(target);
            }
        }
    }

    private boolean isReferenceToAssocItem(@NotNull PsiElement target) {
        List<RsPathResolveResult<RsElement>> resolved = rawMultiResolve();
        com.intellij.psi.PsiManager mgr = target.getManager();
        return resolved.stream().anyMatch(it -> mgr.areElementsEquivalent(it.getElement(), target));
    }

    private boolean isReferenceToNonAssocItem(@NotNull PsiElement target) {
        boolean canBeReferenceTo;
        if (target instanceof RsEnumVariant) {
            RsTypeQual typeQual = getElement().getTypeQual();
            canBeReferenceTo = typeQual == null || typeQual.getTraitRef() == null;
        } else {
            canBeReferenceTo = !isAssociatedItemOrEnumVariantPath(getElement());
        }
        if (!canBeReferenceTo) return false;
        com.intellij.psi.PsiManager mgr = target.getManager();
        List<ScopeEntry> resolved = resolvePathRaw(getElement(), null, false);
        return resolved.stream().anyMatch(it -> mgr.areElementsEquivalent(it.getElement(), target));
    }

    private static boolean isAssociatedItemOrEnumVariantPath(@NotNull RsPath path) {
        if (path.getTypeQual() != null) return true;
        RsPath qualifier = path.getPath();
        if (qualifier == null) return false;
        RsReference qualRef = qualifier.getReference();
        if (qualRef == null) return false;
        PsiElement resolvedQualifier = qualRef.resolve();
        return !(resolvedQualifier instanceof RsMod);
    }

    @Nullable
    @Override
    public BoundElement<RsElement> advancedResolve() {
        List<RsPathResolveResult<RsElement>> resultFromTypeInference = rawMultiResolveUsingInferenceCache();
        if (resultFromTypeInference != null) {
            if (resultFromTypeInference.size() != 1) return null;
            RsPathResolveResult<RsElement> single = resultFromTypeInference.get(0);
            return new BoundElement<>(single.element(), single.getResolvedSubst());
        }

        Map<RsPath, List<RsPathResolveResult<RsElement>>> resolvedNestedPaths = resolveNeighborPaths(getElement());
        List<RsPathResolveResult<RsElement>> list = resolvedNestedPaths.get(getElement());
        if (list == null || list.size() != 1) return null;
        RsPathResolveResult<RsElement> resolved = list.get(0);
        return TyLowering.lowerPathGenerics(
            getElement(),
            resolved.element(),
            resolved.getResolvedSubst(),
            PathExprResolver.getDefault(),
            resolvedNestedPaths
        );
    }

    @Nullable
    @Override
    public RsElement resolve() {
        List<RsPathResolveResult<RsElement>> results = rawMultiResolve();
        if (results.size() != 1) return null;
        return results.get(0).element();
    }

    @NotNull
    @Override
    public ResolveResult[] multiResolve(boolean incompleteCode) {
        List<RsPathResolveResult<RsElement>> results = rawMultiResolve();
        return results.toArray(ResolveResult.EMPTY_ARRAY);
    }

    @NotNull
    @Override
    public List<RsElement> multiResolve() {
        List<RsPathResolveResult<RsElement>> results = rawMultiResolve();
        List<RsElement> elements = new ArrayList<>(results.size());
        for (RsPathResolveResult<RsElement> result : results) {
            elements.add(result.element());
        }
        return elements;
    }

    @NotNull
    @Override
    public List<RsElement> multiResolveIfVisible() {
        List<RsPathResolveResult<RsElement>> results = rawMultiResolve();
        List<RsElement> elements = new ArrayList<>();
        for (RsPathResolveResult<RsElement> result : results) {
            if (result.isVisible()) {
                elements.add(result.element());
            }
        }
        return elements;
    }

    @NotNull
    @Override
    public List<RsPathResolveResult<RsElement>> rawMultiResolve() {
        List<RsPathResolveResult<RsElement>> cached = rawMultiResolveUsingInferenceCache();
        if (cached != null) return cached;
        return rawCachedMultiResolve();
    }

    @Nullable
    private List<RsPathResolveResult<RsElement>> rawMultiResolveUsingInferenceCache() {
        PsiElement parent = getElement().getParent();
        if (!(parent instanceof RsPathExpr)) return null;
        RsPathExpr pathExpr = (RsPathExpr) parent;
        org.rust.lang.core.types.infer.RsInferenceResult inference = ExtensionsUtil.getInference(pathExpr);
        if (inference == null) return null;
        List<ResolvedPath> resolvedPaths = inference.getResolvedPath(pathExpr);
        if (resolvedPaths == null) return null;
        List<RsPathResolveResult<RsElement>> results = new ArrayList<>(resolvedPaths.size());
        for (ResolvedPath result : resolvedPaths) {
            boolean isVisible = !(result instanceof ResolvedPath.Item) || ((ResolvedPath.Item) result).isVisible();
            results.add(new RsPathResolveResult<>(result.getElement(), result.getSubst(), isVisible));
        }
        return results;
    }

    @NotNull
    @SuppressWarnings("unchecked")
    private List<RsPathResolveResult<RsElement>> rawCachedMultiResolve() {
        Object mapOrList = resolveNeighborPathsInternal(getElement());
        if (mapOrList == null) return Collections.emptyList();

        Object rawResult;
        if (mapOrList instanceof Map) {
            rawResult = ((Map<?, ?>) mapOrList).get(getElement());
        } else {
            rawResult = mapOrList;
        }

        if (rawResult instanceof List) {
            return (List<RsPathResolveResult<RsElement>>) rawResult;
        }
        return Collections.emptyList();
    }

    @NotNull
    @Override
    public PsiElement bindToElement(@NotNull PsiElement target) {
        if (target instanceof RsMod) {
            PsiElement result = bindToMod((RsMod) target);
            if (result != null) return result;
        }
        return super.bindToElement(target);
    }

    @Nullable
    private PsiElement bindToMod(@NotNull RsMod target) {
        RsPath element = getElement();
        if (!RsElementUtil.isAtLeastEdition2018(element)) return null;
        String targetPath = RsModUtil.qualifiedNameRelativeTo(target, element.getContainingMod());
        if (targetPath == null) return null;

        // Traverse path prefix to find a reusable part
        RsPath prefix = element;
        while (prefix != null) {
            RsReference prefixRef = prefix.getReference();
            if (prefixRef != null) {
                PsiElement resolved = prefixRef.resolve();
                if (resolved instanceof RsMod) {
                    RsMod mod = (RsMod) resolved;
                    List<RsMod> superMods = RsModExtUtil.getSuperMods(target);
                    if (superMods.contains(mod)) {
                        String modFullPath = RsModUtil.qualifiedNameRelativeTo(mod, element.getContainingMod());
                        String modShortPath = prefix.getText();
                        if (modFullPath != null && targetPath.startsWith(modFullPath)) {
                            targetPath = targetPath.replaceFirst(modFullPath, modShortPath);
                        }
                        break;
                    }
                }
            }
            prefix = prefix.getPath();
        }

        RsPsiFactory psiFactory = new RsPsiFactory(element.getProject(), true, false);
        RsPath elementNew = psiFactory.tryCreatePath(targetPath, org.rust.lang.core.parser.RustParserUtil.PathParsingMode.TYPE);
        if (elementNew == null) return null;
        return element.replace(elementNew);
    }

    // --- Resolver ---

    private static final Function<RsElement, Object> RESOLVER = root -> {
        OpenApiUtil.testAssert(() -> !(root.getParent() instanceof RsPathExpr));
        List<RsPath> allPaths = collectNestedPathsFromRoot(root);
        if (allPaths.isEmpty()) return Collections.<RsPathResolveResult<RsElement>>emptyList();

        PathResolutionContext ctx = new PathResolutionContext(root, false, true, null);

        if (allPaths.size() == 1) {
            RsPath singlePath = allPaths.get(0);
            List<RsPathResolveResult<RsElement>> result = NameResolutionUtil.resolvePath(ctx, singlePath, ctx.classifyPath(singlePath));
            for (RsPathResolveResult<RsElement> r : result) {
                assert !FoldUtil.hasTyInfer(r.getResolvedSubst());
            }
            return result;
        }

        List<Map.Entry<RsPath, RsPathResolveKind>> classifiedPaths = new ArrayList<>();
        for (RsPath p : allPaths) {
            classifiedPaths.add(new AbstractMap.SimpleEntry<>(p, ctx.classifyPath(p)));
        }

        List<Map.Entry<RsPath, RsPathResolveKind>> unqualified = new ArrayList<>();
        List<Map.Entry<RsPath, RsPathResolveKind>> others = new ArrayList<>();
        for (Map.Entry<RsPath, RsPathResolveKind> entry : classifiedPaths) {
            if (entry.getValue() instanceof RsPathResolveKind.UnqualifiedPath) {
                unqualified.add(entry);
            } else {
                others.add(entry);
            }
        }

        // Group unqualified by kind
        Map<RsPathResolveKind, List<RsPath>> kindToPathList = new LinkedHashMap<>();
        for (Map.Entry<RsPath, RsPathResolveKind> entry : unqualified) {
            kindToPathList.computeIfAbsent(entry.getValue(), k -> new ArrayList<>()).add(entry.getKey());
        }

        Map<RsPath, List<RsPathResolveResult<RsElement>>> resolved = new HashMap<>();
        for (Map.Entry<RsPathResolveKind, List<RsPath>> entry : kindToPathList.entrySet()) {
            RsPathResolveKind kind = entry.getKey();
            List<RsPath> paths = entry.getValue();
            Map<RsPath, List<RsPathResolveResult<RsElement>>> multiResult =
                NameResolutionUtil.collectMultiplePathResolveVariants(ctx, paths, processor ->
                    NameResolutionUtil.processPathResolveVariants(ctx, kind, processor)
                );
            for (Map.Entry<RsPath, List<RsPathResolveResult<RsElement>>> e : multiResult.entrySet()) {
                resolved.put(e.getKey(), filterResolveResults(e.getKey(), e.getValue()));
            }
        }
        for (Map.Entry<RsPath, RsPathResolveKind> entry : others) {
            RsPath p = entry.getKey();
            resolved.put(p, resolvePathInternal(ctx, p, entry.getValue()));
        }

        for (List<RsPathResolveResult<RsElement>> l : resolved.values()) {
            for (RsPathResolveResult<RsElement> r : l) {
                assert !FoldUtil.hasTyInfer(r.getResolvedSubst());
            }
        }
        return resolved;
    };

    // --- Companion-like static methods ---

    @VisibleForTesting
    @Nullable
    public static RsElement getRootCachingElement(@NotNull RsPath path) {
        StubElement<?> stub = path.getStub();
        if (stub != null) {
            StubElement<?> rootStub = getRootCachingElementStub(stub);
            return rootStub != null ? (RsElement) rootStub.getPsi() : null;
        } else {
            return getRootCachingElementPsi(path);
        }
    }

    @Nullable
    private static RsElement getRootCachingElementPsi(@NotNull RsPath path) {
        RsElement root = null;
        PsiElement element = path;
        IElementType elementType = RsElementTypes.PATH;
        IElementType prevElementType = null;
        while (true) {
            if (!isStepToParentAllowed(prevElementType, elementType)) break;

            PsiElement parent = element.getParent();
            IElementType parentType = RsPsiJavaUtil.elementType(parent);
            if (parentType == RsElementTypes.PATH_EXPR) break;

            if (ALLOWED_CACHING_ROOTS.contains(elementType)) {
                root = (RsElement) element;
            }
            prevElementType = elementType;
            element = parent;
            elementType = parentType;
        }
        return root;
    }

    @Nullable
    private static StubElement<?> getRootCachingElementStub(@NotNull StubElement<?> path) {
        StubElement<?> root = null;
        StubElement<?> element = path;
        IElementType elementType = RsElementTypes.PATH;
        IElementType prevElementType = null;
        while (true) {
            if (!isStepToParentAllowed(prevElementType, elementType)) break;

            StubElement<?> parent = element.getParentStub();
            IElementType parentType = parent.getStubType();
            if (parentType == RsElementTypes.PATH_EXPR) break;

            if (ALLOWED_CACHING_ROOTS.contains(elementType)) {
                root = element;
            }
            prevElementType = elementType;
            element = parent;
            elementType = parentType;
        }
        return root;
    }

    private static boolean isStepToParentAllowed(@Nullable IElementType child, @NotNull IElementType parent) {
        TokenSet set = (child == RsElementTypes.PATH) ? ALLOWED_PARENT_FOR_PATH : ALLOWED_PARENT_FOR_OTHERS;
        return set.contains(parent);
    }

    private static final TokenSet ALLOWED_CACHING_ROOTS = TokenSet.orSet(
        RsTokenType.RS_TYPES,
        TokenSet.create(RsElementTypes.PATH, RsElementTypes.TYPE_ARGUMENT_LIST)
    );

    private static final TokenSet ALLOWED_PARENT_FOR_PATH = TokenSet.orSet(
        RsTokenType.RS_TYPES,
        TokenSet.create(RsElementTypes.TYPE_ARGUMENT_LIST, RsElementTypes.TRAIT_REF)
    );

    private static final TokenSet ALLOWED_PARENT_FOR_OTHERS = TokenSet.orSet(
        RsTokenType.RS_TYPES,
        TokenSet.create(
            RsElementTypes.TYPE_ARGUMENT_LIST, RsElementTypes.PATH, RsElementTypes.VALUE_PARAMETER,
            RsElementTypes.VALUE_PARAMETER_LIST, RsElementTypes.RET_TYPE, RsElementTypes.TRAIT_REF,
            RsElementTypes.BOUND, RsElementTypes.POLYBOUND, RsElementTypes.ASSOC_TYPE_BINDING
        )
    );

    @VisibleForTesting
    @NotNull
    public static List<RsPath> collectNestedPathsFromRoot(@NotNull RsElement root) {
        StubElement<?> stub = (root instanceof StubBasedPsiElementBase)
            ? ((StubBasedPsiElementBase<?>) root).getStub()
            : null;
        if (stub != null) {
            return collectNestedPathsFromRootStub(stub);
        } else {
            return collectNestedPathsFromRootPsi(root);
        }
    }

    @NotNull
    private static List<RsPath> collectNestedPathsFromRootPsi(@NotNull RsElement root) {
        OpenApiUtil.testAssert(() -> ALLOWED_CACHING_ROOTS.contains(RsPsiJavaUtil.elementType(root)));

        List<RsPath> result = new SmartList<>();
        List<PsiElement> queue = new ArrayList<>();
        queue.add(root);

        while (!queue.isEmpty()) {
            PsiElement nextElement = queue.remove(queue.size() - 1);
            if (nextElement instanceof RsPath) {
                OpenApiUtil.testAssert(() -> getRootCachingElementPsi((RsPath) nextElement) == root);
                result.add((RsPath) nextElement);
            }

            IElementType nextElementType = RsPsiJavaUtil.elementType(nextElement);
            for (PsiElement child = nextElement.getFirstChild(); child != null; child = child.getNextSibling()) {
                if (child instanceof RsElement && isStepToParentAllowed(RsPsiJavaUtil.elementType(child), nextElementType)) {
                    queue.add(child);
                }
            }
        }

        return result;
    }

    @NotNull
    private static List<RsPath> collectNestedPathsFromRootStub(@NotNull StubElement<?> root) {
        OpenApiUtil.testAssert(() -> ALLOWED_CACHING_ROOTS.contains(root.getStubType()));

        List<RsPath> result = new SmartList<>();
        List<StubElement<?>> queue = new ArrayList<>();
        queue.add(root);

        while (!queue.isEmpty()) {
            StubElement<?> nextStub = queue.remove(queue.size() - 1);
            if (nextStub instanceof RsPathStub) {
                StubElement<?> rootRef = root;
                OpenApiUtil.testAssert(() -> getRootCachingElementStub((RsPathStub) nextStub) == rootRef);
                result.add((RsPath) nextStub.getPsi());
            }

            IElementType nextStubType = nextStub.getStubType();
            for (StubElement<?> child : nextStub.getChildrenStubs()) {
                if (isStepToParentAllowed(child.getStubType(), nextStubType)) {
                    queue.add(child);
                }
            }
        }

        return result;
    }

    @Nullable
    private static Object resolveNeighborPathsInternal(@NotNull RsPath path) {
        RsElement root = getRootCachingElement(path);
        if (root == null) return null;
        return RsResolveCache.getInstance(path.getProject())
            .resolveWithCaching(root, ResolveCacheDependency.LOCAL_AND_RUST_STRUCTURE, RESOLVER);
    }

    @NotNull
    @SuppressWarnings("unchecked")
    public static Map<RsPath, List<RsPathResolveResult<RsElement>>> resolveNeighborPaths(@NotNull RsPath path) {
        Object mapOrList = resolveNeighborPathsInternal(path);
        if (mapOrList == null) return Collections.emptyMap();
        if (mapOrList instanceof Map) {
            return (Map<RsPath, List<RsPathResolveResult<RsElement>>>) mapOrList;
        }
        return Collections.singletonMap(path, (List<RsPathResolveResult<RsElement>>) mapOrList);
    }

    // --- Top-level utility functions ---

    @NotNull
    public static List<ScopeEntry> resolvePathRaw(@NotNull RsPath path, @Nullable ImplLookup lookup, boolean resolveAssocItems) {
        return NameResolutionUtil.collectResolveVariantsAsScopeEntries(path.getReferenceName(), processor ->
            NameResolutionUtil.processPathResolveVariants(lookup, path, false, resolveAssocItems, (RsResolveProcessor) processor)
        );
    }

    @NotNull
    private static List<RsPathResolveResult<RsElement>> resolvePathInternal(
        @NotNull PathResolutionContext ctx,
        @NotNull RsPath path,
        @NotNull RsPathResolveKind kind
    ) {
        List<RsPathResolveResult<RsElement>> result = NameResolutionUtil.collectPathResolveVariants(ctx, path, processor ->
            NameResolutionUtil.processPathResolveVariants(ctx, kind, processor)
        );
        return filterResolveResults(path, result);
    }

    @NotNull
    private static List<RsPathResolveResult<RsElement>> filterResolveResults(
        @NotNull RsPath path,
        @NotNull List<RsPathResolveResult<RsElement>> result
    ) {
        PsiElement pathParent = path.getParent();
        if (pathParent instanceof RsTypeReference && pathParent.getParent() instanceof RsTypeArgumentList) {
            if (result.isEmpty()) return Collections.emptyList();
            if (result.size() == 1) return result;
            List<RsPathResolveResult<RsElement>> types = new ArrayList<>();
            for (RsPathResolveResult<RsElement> r : result) {
                if (r.getNamespaces().contains(Namespace.Types)) {
                    types.add(r);
                }
            }
            return types.isEmpty() ? result : types;
        }
        return result;
    }

    @Nullable
    public static RsTypeAlias resolveAssocTypeBinding(@NotNull RsTraitItem trait, @NotNull RsAssocTypeBinding binding) {
        List<RsElement> resolved = NameResolutionUtil.collectResolveVariants(binding.getPath().getReferenceName(), processor ->
            NameResolutionUtil.processAssocTypeVariants(trait, processor)
        );
        if (resolved.size() != 1) return null;
        RsElement single = resolved.get(0);
        return single instanceof RsTypeAlias ? (RsTypeAlias) single : null;
    }

    /** Resolves a reference through type aliases */
    @Nullable
    public static RsElement deepResolve(@NotNull RsPathReference ref) {
        BoundElement<RsElement> bound = advancedDeepResolve(ref);
        return bound != null ? bound.element() : null;
    }

    /** Resolves a reference through type aliases */
    @Nullable
    public static BoundElement<RsElement> advancedDeepResolve(@NotNull RsPathReference ref) {
        BoundElement<RsElement> boundElement = ref.advancedResolve();
        if (boundElement != null) {
            // Resolve potential Self inside impl
            if (boundElement.element() instanceof RsImplItem && RsPathUtil.getHasCself(ref.getElement())) {
                RsTypeReference typeRef = ((RsImplItem) boundElement.element()).getTypeReference();
                if (typeRef != null) {
                    PsiElement skipped = RsTypeReferenceUtil.skipParens(typeRef);
                    if (skipped instanceof RsPathType) {
                        RsPathReference pathRef = (RsPathReference) ((RsPathType) skipped).getPath().getReference();
                        if (pathRef != null) {
                            BoundElement<RsElement> resolved = pathRef.advancedResolve();
                            if (resolved != null) {
                                boundElement = resolved;
                            }
                        }
                    }
                }
            }
        }

        if (boundElement != null && boundElement.element() instanceof RsTypeAlias) {
            return resolveThroughTypeAliases(boundElement);
        }
        return boundElement;
    }

    @Nullable
    private static BoundElement<RsElement> resolveThroughTypeAliases(@NotNull BoundElement<RsElement> boundElement) {
        BoundElement<RsElement> base = boundElement;
        Set<RsElement> visited = new HashSet<>();
        visited.add(boundElement.element());
        while (base.element() instanceof RsTypeAlias) {
            RsTypeReference typeRef = ((RsTypeAlias) base.element()).getTypeReference();
            if (typeRef == null) break;
            PsiElement skipped = RsTypeReferenceUtil.skipParens(typeRef);
            if (!(skipped instanceof RsPathType)) break;
            RsReference pathRef = ((RsPathType) skipped).getPath().getReference();
            if (!(pathRef instanceof RsPathReference)) break;
            BoundElement<RsElement> resolved = ((RsPathReference) pathRef).advancedResolve();
            if (resolved == null) break;
            if (!visited.add(resolved.element())) return null;
            // Stop at type S<T> = T;
            if (resolved.element() instanceof RsTypeParameter) break;
            base = FoldUtil.substitute(resolved, base.getSubst());
        }
        return base;
    }

    /** @see #tryAdvancedResolveTypeAliasToImpl */
    @Nullable
    public static BoundElement<RsElement> advancedResolveTypeAliasToImpl(@NotNull RsPathReference ref) {
        BoundElement<RsElement> result = tryAdvancedResolveTypeAliasToImpl(ref);
        return result != null ? result : ref.advancedResolve();
    }

    /** @see #tryAdvancedResolveTypeAliasToImpl */
    @Nullable
    public static RsElement tryResolveTypeAliasToImpl(@NotNull RsPathReference ref) {
        BoundElement<RsElement> result = tryAdvancedResolveTypeAliasToImpl(ref);
        return result != null ? result.element() : null;
    }

    /** @see #tryAdvancedResolveTypeAliasToImpl */
    @Nullable
    public static RsElement resolveTypeAliasToImpl(@NotNull RsPathReference ref) {
        RsElement result = tryResolveTypeAliasToImpl(ref);
        return result != null ? result : ref.resolve();
    }

    @Nullable
    private static BoundElement<RsElement> tryAdvancedResolveTypeAliasToImpl(@NotNull RsPathReference ref) {
        BoundElement<RsElement> resolvedBoundElement = ref.advancedResolve();
        if (resolvedBoundElement == null) return null;

        // 1. Check that we're resolved to an associated type inside a trait
        RsElement resolved = resolvedBoundElement.element();
        if (!(resolved instanceof RsTypeAlias)) return null;
        RsTypeAlias resolvedAlias = (RsTypeAlias) resolved;
        if (!(RsAbstractableOwnerUtil.getOwner(resolvedAlias) instanceof RsAbstractableOwner.Trait)) return null;

        // 2. Check that we resolve a `Self`-qualified path or an explicit type-qualified path
        RsTypeQual typeQual = ref.getElement().getTypeQual();
        RsPath parentPath = ref.getElement().getPath();
        boolean hasCself = parentPath != null && RsPathUtil.getHasCself(parentPath);
        if (!hasCself && (typeQual == null || typeQual.getTraitRef() == null)) return null;

        // 3. Try to select a concrete impl for the associated type
        ImplLookup lookup = ImplLookup.relativeTo(ref.getElement());
        TyProjection projection = (TyProjection) FoldUtil.substitute(
            TyProjection.valueOf(RsGenericDeclarationUtil.withDefaultSubst(resolvedAlias)),
            resolvedBoundElement.getSubst()
        );
        org.rust.lang.core.resolve.Selection selection =
            lookup.selectStrict(projection.getTraitRef()).ok();
        if (selection == null) return null;
        if (!(selection.getImpl() instanceof org.rust.lang.core.psi.RsImplItem)) return null;
        org.rust.lang.core.psi.RsImplItem impl = (org.rust.lang.core.psi.RsImplItem) selection.getImpl();

        RsTypeAlias matched = null;
        for (org.rust.lang.core.psi.ext.RsAbstractable m :
             org.rust.lang.core.psi.ext.RsTraitOrImplUtil.getExpandedMembers(impl)) {
            if (m instanceof RsTypeAlias && java.util.Objects.equals(
                ((RsTypeAlias) m).getName(), resolvedAlias.getName())) {
                matched = (RsTypeAlias) m;
                break;
            }
        }
        if (matched == null) return null;

        org.rust.lang.core.types.Substitution newSubst =
            lookup.getCtx().fullyResolveWithOrigins(selection.getSubst());
        org.rust.lang.core.resolve.NameResolutionTestmarks.TypeAliasToImpl.hit();
        return new BoundElement<>(matched, newSubst);
    }
}
