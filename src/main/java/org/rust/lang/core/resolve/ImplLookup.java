/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.lang.core.resolve;

import org.rust.stdext.Lazy;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.Key;
import com.intellij.psi.util.CachedValue;
import com.intellij.psi.util.CachedValueProvider;
import com.intellij.psi.util.CachedValuesManager;
import com.intellij.psi.util.PsiModificationTracker;
import com.intellij.util.SmartList;
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.rust.cargo.project.workspace.PackageOrigin;
import org.rust.lang.core.crate.Crate;
import org.rust.lang.core.psi.*;
import org.rust.lang.core.psi.ext.*;
import org.rust.lang.core.types.*;
import org.rust.lang.core.types.consts.Const;
import org.rust.lang.core.types.consts.CtConstParameter;
import org.rust.lang.core.types.consts.CtInferVar;
import org.rust.lang.core.types.consts.FreshCtInferVar;
import org.rust.lang.core.types.infer.*;
import org.rust.lang.core.types.infer.FoldUtil;
import org.rust.lang.core.types.ty.*;
import org.rust.lang.utils.evaluation.ThreeValuedLogic;
import org.rust.openapiext.Testmark;
import org.rust.stdext.CollectionsUtil;

import java.util.*;
import java.util.stream.Collectors;

public class ImplLookup {

    public static final int DEFAULT_RECURSION_LIMIT = 128;

    @NotNull
    private final Project project;
    @NotNull
    private final Crate containingCrate;
    @NotNull
    private final KnownItems items;
    @NotNull
    private final ParamEnv paramEnv;
    @Nullable
    private final RsElement context;
    @NotNull
    private final TypeInferenceOptions options;

    private final Map<TraitRef, SelectionResult<SelectionCandidate>> traitSelectionCache = new HashMap<>();
    private final Map<Ty, List<TraitImplSource>> findImplsAndTraitsCache = new HashMap<>();
    private final RsImplIndexAndTypeAliasCache indexCache;
    private final List<RsTraitItem> fnTraits;

    // Lazy fields
    private volatile RsTypeAlias fnOnceOutput;
    private volatile boolean fnOnceOutputInitialized;

    private final Pair<RsTraitItem, RsTypeAlias> derefTraitAndTarget;

    private volatile Pair<RsTraitItem, RsTypeAlias> indexTraitAndOutput;
    private volatile boolean indexTraitAndOutputInitialized;

    private volatile Pair<RsTraitItem, RsTypeAlias> intoIteratorTraitAndOutput;
    private volatile boolean intoIteratorTraitAndOutputInitialized;

    private volatile Map<TyFingerprint, List<RsCachedImplItem>> implsFromNestedMacros;
    private volatile boolean implsFromNestedMacrosInitialized;

    private volatile ImplsFilter implsFilter;
    private volatile boolean implsFilterInitialized;

    private volatile RsInferenceContext ctx;

    public ImplLookup(
        @NotNull Project project,
        @NotNull Crate containingCrate,
        @NotNull KnownItems items,
        @NotNull ParamEnv paramEnv
    ) {
        this(project, containingCrate, items, paramEnv, null, TypeInferenceOptions.DEFAULT);
    }

    public ImplLookup(
        @NotNull Project project,
        @NotNull Crate containingCrate,
        @NotNull KnownItems items,
        @NotNull ParamEnv paramEnv,
        @Nullable RsElement context,
        @NotNull TypeInferenceOptions options
    ) {
        this.project = project;
        this.containingCrate = containingCrate;
        this.items = items;
        this.paramEnv = paramEnv;
        this.context = context;
        this.options = options;
        this.indexCache = RsImplIndexAndTypeAliasCache.getInstance(project);

        List<RsTraitItem> fnTraitsList = new ArrayList<>();
        if (items.getFn() != null) fnTraitsList.add(items.getFn());
        if (items.getFnMut() != null) fnTraitsList.add(items.getFnMut());
        if (items.getFnOnce() != null) fnTraitsList.add(items.getFnOnce());
        this.fnTraits = fnTraitsList;

        RsTraitItem derefTrait = items.getDeref();
        if (derefTrait != null) {
            RsTypeAlias target = RsTraitItemUtil.findAssociatedType(derefTrait, "Target");
            this.derefTraitAndTarget = target != null ? new Pair<>(derefTrait, target) : null;
        } else {
            this.derefTraitAndTarget = null;
        }
    }

    @NotNull
    public KnownItems getItems() {
        return items;
    }

    @NotNull
    public RsInferenceContext getCtx() {
        if (ctx == null) {
            synchronized (this) {
                if (ctx == null) {
                    ctx = new RsInferenceContext(project, this, items, options);
                }
            }
        }
        return ctx;
    }

    @Nullable
    private RsTypeAlias getFnOnceOutput() {
        if (!fnOnceOutputInitialized) {
            RsTraitItem trait = items.getFnOnce();
            if (trait != null) {
                fnOnceOutput = RsTraitItemUtil.findAssociatedType(trait, "Output");
            }
            fnOnceOutputInitialized = true;
        }
        return fnOnceOutput;
    }

    @Nullable
    private Pair<RsTraitItem, RsTypeAlias> getIndexTraitAndOutput() {
        if (!indexTraitAndOutputInitialized) {
            RsTraitItem trait = items.getIndex();
            if (trait != null) {
                RsTypeAlias output = RsTraitItemUtil.findAssociatedType(trait, "Output");
                if (output != null) {
                    indexTraitAndOutput = new Pair<>(trait, output);
                }
            }
            indexTraitAndOutputInitialized = true;
        }
        return indexTraitAndOutput;
    }

    @Nullable
    private Pair<RsTraitItem, RsTypeAlias> getIntoIteratorTraitAndOutput() {
        if (!intoIteratorTraitAndOutputInitialized) {
            RsTraitItem trait = items.getIntoIterator();
            if (trait != null) {
                RsTypeAlias item = RsTraitItemUtil.findAssociatedType(trait, "Item");
                if (item != null) {
                    intoIteratorTraitAndOutput = new Pair<>(trait, item);
                }
            }
            intoIteratorTraitAndOutputInitialized = true;
        }
        return intoIteratorTraitAndOutput;
    }

    @NotNull
    private Map<TyFingerprint, List<RsCachedImplItem>> getImplsFromNestedMacros() {
        if (!implsFromNestedMacrosInitialized) {
            if (context == null) {
                implsFromNestedMacros = Collections.emptyMap();
            } else {
                implsFromNestedMacros = doGetImplsFromNestedMacros(context);
            }
            implsFromNestedMacrosInitialized = true;
        }
        return implsFromNestedMacros;
    }

    @NotNull
    private ImplsFilter getImplsFilter() {
        if (!implsFilterInitialized) {
            implsFilter = computeImplsFilter();
            implsFilterInitialized = true;
        }
        return implsFilter;
    }

    @NotNull
    private ImplsFilter computeImplsFilter() {
        // Simplified: return AllowAll by default
        return ImplsFilter.AllowAll.INSTANCE;
    }

    @NotNull
    public ParamEnv.Sequence<BoundElement<RsTraitItem>> getEnvBoundTransitivelyFor(@NotNull Ty ty) {
        return paramEnv.boundsFor(ty);
    }

    /**
     * Resulting sequence is ordered: inherent impls are placed to the head.
     */
    @NotNull
    public List<TraitImplSource> findImplsAndTraits(@NotNull Ty ty) {
        Ty fresh = freshen(ty);
        List<TraitImplSource> cached = findImplsAndTraitsCache.get(fresh);
        if (cached == null) {
            cached = rawFindImplsAndTraits(ty);
            findImplsAndTraitsCache.put(fresh, cached);
        }

        boolean isInherentBounds = ty instanceof TyTypeParameter;
        List<TraitImplSource> envBounds = new ArrayList<>();
        for (BoundElement<RsTraitItem> be : getEnvBoundTransitivelyFor(ty)) {
            envBounds.add(new TraitImplSource.TraitBound(be.getTypedElement(), isInherentBounds));
        }

        if (isInherentBounds) {
            List<TraitImplSource> result = new ArrayList<>(envBounds);
            result.addAll(cached);
            return result;
        } else {
            List<TraitImplSource> result = new ArrayList<>(cached);
            result.addAll(envBounds);
            return result;
        }
    }

    @NotNull
    private List<TraitImplSource> rawFindImplsAndTraits(@NotNull Ty ty) {
        List<TraitImplSource> implsAndTraits = new ArrayList<>();
        if (ty instanceof TyTraitObject) {
            for (BoundElement<RsTraitItem> be : ((TyTraitObject) ty).getTraits()) {
                implsAndTraits.add(new TraitImplSource.Object(be.getTypedElement()));
            }
            findExplicitImpls(ty, impl -> {
                implsAndTraits.add(impl.getExplicitImpl());
                return false;
            });
        } else if (ty instanceof TyFunctionBase) {
            findExplicitImpls(ty, impl -> {
                implsAndTraits.add(impl.getExplicitImpl());
                return false;
            });
            for (RsTraitItem fnTrait : fnTraits) {
                implsAndTraits.add(new TraitImplSource.Object(fnTrait));
            }
            if (items.getClone() != null) implsAndTraits.add(new TraitImplSource.Builtin(items.getClone()));
            if (items.getCopy() != null) implsAndTraits.add(new TraitImplSource.Builtin(items.getCopy()));
        } else if (ty instanceof TyAnon) {
            Set<RsTraitItem> seen = new HashSet<>();
            for (BoundElement<RsTraitItem> be : ((TyAnon) ty).getTraits()) {
                if (seen.add(be.getTypedElement())) {
                    implsAndTraits.add(new TraitImplSource.Object(be.getTypedElement()));
                }
            }
            for (RsCachedImplItem blanketImpl : findBlanketImpls()) {
                if (!blanketImpl.isNegativeImpl()) {
                    implsAndTraits.add(blanketImpl.getExplicitImpl());
                }
            }
        } else if (ty instanceof TyProjection) {
            // Simplified - add projection bounds
        } else if (!(ty instanceof TyUnknown)) {
            implsAndTraits.addAll(findDerivedTraits(ty).stream()
                .map(TraitImplSource.Derived::new)
                .collect(Collectors.toList()));
            findExplicitImpls(ty, impl -> {
                implsAndTraits.add(impl.getExplicitImpl());
                return false;
            });
            if (ty instanceof TyTuple || ty instanceof TyUnit) {
                if (items.getClone() != null) implsAndTraits.add(new TraitImplSource.Builtin(items.getClone()));
                if (items.getCopy() != null) implsAndTraits.add(new TraitImplSource.Builtin(items.getCopy()));
            }
        }
        // Place inherent impls to the head of the list
        implsAndTraits.sort((a, b) -> Boolean.compare(!a.isInherent(), !b.isInherent()));
        return implsAndTraits;
    }

    @NotNull
    private Collection<RsTraitItem> findDerivedTraits(@NotNull Ty ty) {
        if (ty instanceof TyAdt) {
            Collection<RsTraitItem> derived = RsStructOrEnumItemElementUtil.getDerivedTraits(((TyAdt) ty).getItem());
            if (derived == null) return Collections.emptyList();
            return derived.stream()
                .filter(RsTraitItemImplUtil::isKnownDerivable)
                .collect(Collectors.toList());
        }
        return Collections.emptyList();
    }

    private boolean findExplicitImpls(@NotNull Ty selfTy, @NotNull RsProcessor<RsCachedImplItem> processor) {
        return processTyFingerprintsWithAliases(selfTy, tyFingerprint ->
            findExplicitImplsWithoutAliases(selfTy, tyFingerprint, processor));
    }

    private boolean findExplicitImplsWithoutAliases(
        @NotNull Ty selfTy,
        @NotNull TyFingerprint tyf,
        @NotNull RsProcessor<RsCachedImplItem> processor
    ) {
        for (RsCachedImplItem cachedImpl : findPotentialImpls(tyf)) {
            if (cachedImpl.isNegativeImpl()) continue;
            Ty type = cachedImpl.getType();
            List<TyTypeParameter> generics = cachedImpl.getGenerics();
            List<CtConstParameter> constGenerics = cachedImpl.getConstGenerics();
            if (type == null || generics == null || constGenerics == null) continue;
            boolean isAppropriateImpl = canCombineTypes(selfTy, type, generics, constGenerics)
                && (cachedImpl.isInherent() || cachedImpl.getImplementedTrait() != null);
            if (isAppropriateImpl && processor.process(cachedImpl)) return true;
        }
        return false;
    }

    private boolean processTyFingerprintsWithAliases(@NotNull Ty selfTy, @NotNull RsProcessor<TyFingerprint> processor) {
        TyFingerprint fingerprint = TyFingerprint.create(selfTy);
        if (fingerprint != null) {
            Set<TyFingerprint> set = new HashSet<>();
            set.add(fingerprint);
            if (processor.process(fingerprint)) return true;
            List<String> aliases = findPotentialAliases(fingerprint);
            for (String name : aliases) {
                TyFingerprint aliasFingerprint = new TyFingerprint(name);
                if (set.add(aliasFingerprint) && processor.process(aliasFingerprint)) return true;
            }
        }
        return processor.process(TyFingerprint.TYPE_PARAMETER_OR_MACRO_FINGERPRINT);
    }

    @NotNull
    private List<RsCachedImplItem> findPotentialImpls(@NotNull TyFingerprint tyf) {
        List<RsCachedImplItem> result = new ArrayList<>();
        for (RsCachedImplItem impl : indexCache.findPotentialImpls(tyf)) {
            if (useImplsFromCrate(impl.getContainingCrates()) && getImplsFilter().canProcessImpl(impl)) {
                result.add(impl);
            }
        }
        List<RsCachedImplItem> nested = getImplsFromNestedMacros().get(tyf);
        if (nested != null) {
            for (RsCachedImplItem impl : nested) {
                if (getImplsFilter().canProcessImpl(impl)) {
                    result.add(impl);
                }
            }
        }
        return result;
    }

    @NotNull
    private List<String> findPotentialAliases(@NotNull TyFingerprint tyf) {
        return indexCache.findPotentialAliases(tyf);
    }

    private boolean useImplsFromCrate(@NotNull List<Crate> crates) {
        for (Crate c : crates) {
            if (Crate.hasTransitiveDependencyOrSelf(containingCrate, c)) return true;
        }
        return false;
    }

    private boolean canCombineTypes(
        @NotNull Ty ty1,
        @NotNull Ty ty2,
        @NotNull List<TyTypeParameter> genericsForTy2,
        @NotNull List<CtConstParameter> constGenericsForTy2
    ) {
        if (genericsForTy2.size() < 5) {
            if (genericsForTy2.contains(ty2)) return true;
            if (ty2 instanceof TyReference && genericsForTy2.contains(((TyReference) ty2).getReferenced())) {
                return ty1 instanceof TyReference && ((TyReference) ty1).getMutability() == ((TyReference) ty2).getMutability();
            }
        }

        Map<TyTypeParameter, Ty> typeSubst = new HashMap<>();
        for (TyTypeParameter param : genericsForTy2) {
            typeSubst.put(param, getCtx().typeVarForParam(param));
        }
        Map<CtConstParameter, Const> constSubst = new HashMap<>();
        for (CtConstParameter param : constGenericsForTy2) {
            constSubst.put(param, getCtx().constVarForParam(param));
        }

        return getCtx().probe(() -> {
            Ty normTy2 = getCtx().normalizeAssociatedTypesIn(FoldUtil.substitute(ty2, new Substitution(typeSubst, Collections.emptyMap(), constSubst))).getValue();
            return getCtx().combineTypes(normTy2, ty1).isOk();
        });
    }

    @NotNull
    private List<RsCachedImplItem> findBlanketImpls() {
        return findPotentialImpls(TyFingerprint.TYPE_PARAMETER_OR_MACRO_FINGERPRINT);
    }

    public boolean canSelectWithDeref(@NotNull TraitRef ref) {
        return canSelectWithDeref(ref, 0);
    }

    public boolean canSelectWithDeref(@NotNull TraitRef ref, int recursionDepth) {
        for (Ty ty : coercionSequence(ref.getSelfTy())) {
            if (canSelect(new TraitRef(ty, ref.getTrait()), recursionDepth)) return true;
        }
        return false;
    }

    public boolean canSelect(@NotNull TraitRef ref) {
        return canSelect(ref, 0);
    }

    public boolean canSelect(@NotNull TraitRef ref, int recursionDepth) {
        return selectStrictWithoutConfirm(ref, recursionDepth).isOk();
    }

    @NotNull
    public SelectionResult<Selection> selectStrict(@NotNull TraitRef ref) {
        return selectStrict(ref, 0);
    }

    @NotNull
    public SelectionResult<Selection> selectStrict(@NotNull TraitRef ref, int recursionDepth) {
        SelectionResult<SelectionCandidate> result = selectStrictWithoutConfirm(ref, recursionDepth);
        return result.andThen(candidate -> confirmCandidate(ref, candidate, recursionDepth));
    }

    @NotNull
    private SelectionResult<SelectionCandidate> selectStrictWithoutConfirm(@NotNull TraitRef ref, int recursionDepth) {
        SelectionResult<SelectionCandidate> result = selectWithoutConfirm(ref, BoundConstness.NotConst, recursionDepth);
        SelectionCandidate candidate = result.ok();
        if (candidate == null) return result.map(c -> { throw new IllegalStateException("unreachable"); });
        if (!canEvaluateObligations(ref, candidate, recursionDepth)) return SelectionResult.err();
        return result;
    }

    @NotNull
    public SelectionResult<Selection> select(@NotNull TraitRef ref) {
        return select(ref, 0);
    }

    @NotNull
    public SelectionResult<Selection> select(@NotNull TraitRef ref, int recursionDepth) {
        return select(ref, BoundConstness.NotConst, recursionDepth);
    }

    @NotNull
    public SelectionResult<Selection> select(@NotNull TraitRef ref, @NotNull BoundConstness constness, int recursionDepth) {
        return selectWithoutConfirm(ref, constness, recursionDepth)
            .andThen(candidate -> confirmCandidate(ref, candidate, recursionDepth));
    }

    @NotNull
    private SelectionResult<SelectionCandidate> selectWithoutConfirm(
        @NotNull TraitRef ref,
        @NotNull BoundConstness constness,
        int recursionDepth
    ) {
        if (recursionDepth > DEFAULT_RECURSION_LIMIT) {
            TypeInferenceMarks.TraitSelectionOverflow.hit();
            return SelectionResult.err();
        }

        if (constness == BoundConstness.ConstIfConst && ref.getTrait().getTypedElement() == items.getDrop()) {
            return SelectionResult.ok(new SelectionCandidate.ParamCandidate(new BoundElement<>(ref.getTrait().getTypedElement())));
        }

        TraitRef fresh = freshen(ref);
        SelectionResult<SelectionCandidate> cached = traitSelectionCache.get(fresh);
        if (cached != null) return cached;
        SelectionResult<SelectionCandidate> result = selectCandidate(ref, recursionDepth);
        traitSelectionCache.put(fresh, result);
        return result;
    }

    @NotNull
    private SelectionResult<SelectionCandidate> selectCandidate(@NotNull TraitRef ref, int recursionDepth) {
        if (ref.getSelfTy() instanceof TyInfer.TyVar) {
            return SelectionResult.ambiguous();
        }
        if (ref.getSelfTy() instanceof TyReference && ((TyReference) ref.getSelfTy()).getReferenced() instanceof TyInfer.TyVar) {
            return SelectionResult.ambiguous();
        }

        // Simplified candidate selection
        return SelectionResult.err();
    }

    @SuppressWarnings("unchecked")
    private <T extends TypeFoldable<T>> T freshen(@NotNull T ty) {
        // Simplified freshening for cache key purposes
        return ty;
    }

    private boolean canEvaluateObligations(@NotNull TraitRef ref, @NotNull SelectionCandidate candidate, int recursionDepth) {
        return getCtx().probe(() -> {
            SelectionResult<Selection> selResult = confirmCandidate(ref, candidate, recursionDepth);
            Selection selection = selResult.ok();
            if (selection == null) return false;
            FulfillmentContext ff = new FulfillmentContext(getCtx(), this);
            for (Obligation obligation : selection.getNestedObligations()) {
                ff.registerPredicateObligation(obligation);
            }
            return ff.selectUntilError();
        });
    }

    @NotNull
    private SelectionResult<Selection> confirmCandidate(
        @NotNull TraitRef ref,
        @NotNull SelectionCandidate candidate,
        int recursionDepth
    ) {
        if (candidate instanceof SelectionCandidate.ParamCandidate) {
            return SelectionResult.ok(confirmParamCandidate(ref, (SelectionCandidate.ParamCandidate) candidate));
        }
        if (candidate instanceof SelectionCandidate.ImplCandidate) {
            return SelectionResult.ok(confirmImplCandidate(ref, (SelectionCandidate.ImplCandidate) candidate, recursionDepth));
        }
        // Default: return the trait element with no nested obligations
        return SelectionResult.ok(new Selection(ref.getTrait().getTypedElement(), Collections.emptyList()));
    }

    @NotNull
    private Selection confirmParamCandidate(@NotNull TraitRef ref, @NotNull SelectionCandidate.ParamCandidate candidate) {
        getCtx().combineBoundElements(candidate.getBound(), ref.getTrait());
        return new Selection(candidate.getBound().getTypedElement(), Collections.emptyList());
    }

    @NotNull
    private Selection confirmImplCandidate(
        @NotNull TraitRef ref,
        @NotNull SelectionCandidate.ImplCandidate candidate,
        int recursionDepth
    ) {
        if (candidate instanceof SelectionCandidate.ImplCandidate.DerivedTrait) {
            return confirmDerivedCandidate(ref, (SelectionCandidate.ImplCandidate.DerivedTrait) candidate, recursionDepth);
        }
        // Default
        return new Selection(ref.getTrait().getTypedElement(), Collections.emptyList());
    }

    @NotNull
    private Selection confirmDerivedCandidate(
        @NotNull TraitRef ref,
        @NotNull SelectionCandidate.ImplCandidate.DerivedTrait candidate,
        int recursionDepth
    ) {
        TyAdt selfTy = (TyAdt) ref.getSelfTy();
        List<Obligation> obligations = new ArrayList<>();
        for (Ty typeArg : selfTy.getTypeArguments()) {
            obligations.add(new Obligation(
                recursionDepth + 1,
                new Predicate.Trait(new TraitRef(typeArg, new BoundElement<>(candidate.getItem())))
            ));
        }
        return new Selection(candidate.getItem(), obligations);
    }

    @NotNull
    public Autoderef coercionSequence(@NotNull Ty baseTy) {
        return new Autoderef(this, getCtx(), baseTy);
    }

    @Nullable
    public TyWithObligations<Ty> deref(@NotNull Ty ty) {
        if (ty instanceof TyReference) {
            return new TyWithObligations<>(((TyReference) ty).getReferenced());
        }
        if (ty instanceof TyPointer) {
            return new TyWithObligations<>(((TyPointer) ty).getReferenced());
        }
        return findDerefTarget(ty);
    }

    @Nullable
    private TyWithObligations<Ty> findDerefTarget(@NotNull Ty ty) {
        if (derefTraitAndTarget == null) return null;
        SelectionResult<TyWithObligations<Ty>> result = selectProjection(derefTraitAndTarget, ty);
        return result.ok();
    }

    @Nullable
    public TyWithObligations<Ty> findIteratorItemType(@NotNull Ty ty) {
        Pair<RsTraitItem, RsTypeAlias> pair = getIntoIteratorTraitAndOutput();
        if (pair == null) return null;
        SelectionResult<TyWithObligations<Ty>> result = selectProjection(pair, ty);
        return result.ok();
    }

    @Nullable
    public TyWithObligations<Ty> findIndexOutputType(@NotNull Ty containerType, @NotNull Ty indexType) {
        Pair<RsTraitItem, RsTypeAlias> pair = getIndexTraitAndOutput();
        if (pair == null) return null;
        SelectionResult<TyWithObligations<Ty>> result = selectProjection(pair, containerType, indexType);
        return result.ok();
    }

    @Nullable
    public TyWithObligations<Ty> findArithmeticBinaryExprOutputType(@NotNull Ty lhsType, @NotNull Ty rhsType, @NotNull ArithmeticOp op) {
        RsTraitItem trait = op.findTrait(items);
        if (trait == null) return null;
        RsTypeAlias assocType = RsTraitItemUtil.findAssociatedType(trait, "Output");
        if (assocType == null) return null;
        TyProjection projection = TyProjection.valueOf(lhsType, new BoundElement<>(assocType).withSubst(rhsType));
        return getCtx().normalizeAssociatedTypesIn(projection);
    }

    @NotNull
    private SelectionResult<TyWithObligations<Ty>> selectProjection(
        @NotNull Pair<RsTraitItem, RsTypeAlias> traitAndOutput,
        @NotNull Ty selfTy,
        @NotNull Ty... subst
    ) {
        TraitRef ref = new TraitRef(selfTy, new BoundElement<>(traitAndOutput.getFirst()).withSubst(subst));
        return selectProjection(ref, new BoundElement<>(traitAndOutput.getSecond()).withSubst());
    }

    @NotNull
    public SelectionResult<TyWithObligations<Ty>> selectProjection(
        @NotNull TraitRef ref,
        @NotNull BoundElement<RsTypeAlias> assocType
    ) {
        return selectProjection(ref, assocType, 0);
    }

    @NotNull
    public SelectionResult<TyWithObligations<Ty>> selectProjection(
        @NotNull TraitRef ref,
        @NotNull BoundElement<RsTypeAlias> assocType,
        int recursionDepth
    ) {
        return select(ref, recursionDepth).map(selection -> {
            Ty looked = lookupAssociatedType(ref.getSelfTy(), selection, assocType);
            if (looked == null) return null;
            TyWithObligations<Ty> normalized = getCtx().normalizeAssociatedTypesIn(looked, recursionDepth + 1);
            return normalized.withObligations(selection.getNestedObligations());
        });
    }

    @NotNull
    public SelectionResult<TyWithObligations<Ty>> selectProjection(
        @NotNull TyProjection projectionTy
    ) {
        return selectProjection(projectionTy, 0);
    }

    @NotNull
    public SelectionResult<TyWithObligations<Ty>> selectProjection(
        @NotNull TyProjection projectionTy,
        int recursionDepth
    ) {
        return selectProjection(projectionTy.getTraitRef(), projectionTy.getTarget(), recursionDepth);
    }

    @NotNull
    public SelectionResult<TyWithObligations<Ty>> selectProjectionStrict(
        @NotNull TraitRef ref,
        @NotNull BoundElement<RsTypeAlias> assocType
    ) {
        return selectProjectionStrict(ref, assocType, 0);
    }

    @NotNull
    public SelectionResult<TyWithObligations<Ty>> selectProjectionStrict(
        @NotNull TraitRef ref,
        @NotNull BoundElement<RsTypeAlias> assocType,
        int recursionDepth
    ) {
        return selectStrict(ref, recursionDepth).map(selection -> {
            Ty looked = lookupAssociatedType(ref.getSelfTy(), selection, assocType);
            if (looked == null) return null;
            TyWithObligations<Ty> normalized = getCtx().normalizeAssociatedTypesIn(looked, recursionDepth + 1);
            return normalized.withObligations(selection.getNestedObligations());
        });
    }

    @NotNull
    public SelectionResult<TyWithObligations<Ty>> selectProjectionStrictWithDeref(
        @NotNull TraitRef ref,
        @NotNull BoundElement<RsTypeAlias> assocType
    ) {
        return selectProjectionStrictWithDeref(ref, assocType, 0);
    }

    @NotNull
    public SelectionResult<TyWithObligations<Ty>> selectProjectionStrictWithDeref(
        @NotNull TraitRef ref,
        @NotNull BoundElement<RsTypeAlias> assocType,
        int recursionDepth
    ) {
        for (Ty ty : coercionSequence(ref.getSelfTy())) {
            SelectionResult<TyWithObligations<Ty>> result = selectProjectionStrict(new TraitRef(ty, ref.getTrait()), assocType, recursionDepth);
            if (result.isOk()) return result;
        }
        return SelectionResult.err();
    }

    @Nullable
    public Map<RsTypeAlias, Ty> selectAllProjectionsStrict(@NotNull TraitRef ref) {
        return getCtx().probe(() -> {
            Selection selection = select(ref).ok();
            if (selection == null) return null;
            Map<RsTypeAlias, TyWithObligations<Ty>> assocValues = new LinkedHashMap<>();
            for (RsTypeAlias assocType : ref.getTrait().getTypedElement().getAssociatedTypesTransitively()) {
                Ty looked = lookupAssociatedType(ref.getSelfTy(), selection, new BoundElement<>(assocType));
                TyWithObligations<Ty> normalized;
                if (looked != null) {
                    normalized = getCtx().normalizeAssociatedTypesIn(looked);
                    normalized = normalized.withObligations(selection.getNestedObligations());
                } else {
                    normalized = new TyWithObligations<>(TyUnknown.INSTANCE);
                }
                assocValues.put(assocType, normalized);
            }
            FulfillmentContext fulfill = new FulfillmentContext(getCtx(), this);
            for (TyWithObligations<Ty> v : assocValues.values()) {
                for (Obligation o : v.getObligations()) {
                    fulfill.registerPredicateObligation(o);
                }
            }
            if (fulfill.selectUntilError()) {
                Map<RsTypeAlias, Ty> result = new LinkedHashMap<>();
                for (Map.Entry<RsTypeAlias, TyWithObligations<Ty>> entry : assocValues.entrySet()) {
                    result.put(entry.getKey(), getCtx().resolveTypeVarsIfPossible(entry.getValue().getValue()));
                }
                return result;
            }
            return null;
        });
    }

    @Nullable
    private Ty lookupAssociatedType(@NotNull Ty selfTy, @NotNull Selection res, @NotNull BoundElement<RsTypeAlias> assocType) {
        if (res.getImpl() instanceof RsImplItem) {
            return lookupAssocTypeInSelection(res, assocType);
        }
        if (selfTy instanceof TyTypeParameter) {
            return lookupAssocTypeInBounds(getEnvBoundTransitivelyFor(selfTy), res.getImpl(), assocType);
        }
        if (selfTy instanceof TyTraitObject) {
            return lookupAssocTypeInBounds(((TyTraitObject) selfTy).getTraits(), res.getImpl(), assocType);
        }
        Ty fromSelection = lookupAssocTypeInSelection(res, assocType);
        if (fromSelection != null) return fromSelection;
        return null;
    }

    @Nullable
    private Ty lookupAssocTypeInSelection(@NotNull Selection selection, @NotNull BoundElement<RsTypeAlias> assocDef) {
        RsTypeAlias assocImpl = null;
        for (RsTypeAlias ta : selection.getImpl().getAssociatedTypesTransitively()) {
            if (ta.getName() != null && ta.getName().equals(assocDef.getTypedElement().getName())) {
                assocImpl = ta;
                break;
            }
        }
        if (assocImpl == null) return null;
        RsTypeReference typeRef = assocImpl.getTypeReference();
        if (typeRef == null) return null;
        return FoldUtil.substitute(ExtensionsUtil.getRawType(typeRef), selection.getSubst().plus(assocDef.getSubst()));
    }

    @Nullable
    private Ty lookupAssocTypeInBounds(
        @NotNull Iterable<BoundElement<RsTraitItem>> subst,
        @NotNull RsTraitOrImpl trait,
        @NotNull BoundElement<RsTypeAlias> assocType
    ) {
        for (BoundElement<RsTraitItem> be : subst) {
            if (be.getTypedElement() == trait) {
                Ty result = be.getAssoc().get(assocType.getTypedElement());
                if (result != null) {
                    return FoldUtil.substitute(result, assocType.getSubst());
                }
            }
        }
        return null;
    }

    @Nullable
    public RsTraitOrImpl findOverloadedOpImpl(@NotNull Ty lhsType, @NotNull Ty rhsType, @NotNull OverloadableBinaryOperator op) {
        RsTraitItem trait = op.findTrait(items);
        if (trait == null) return null;
        Selection selection = select(new TraitRef(lhsType, new BoundElement<>(trait).withSubst(rhsType))).ok();
        return selection != null ? selection.getImpl() : null;
    }

    @Nullable
    public TyWithObligations<TyFunctionBase> asTyFunction(@NotNull Ty ty) {
        if (ty instanceof TyFunctionBase) {
            return new TyWithObligations<>((TyFunctionBase) ty);
        }
        RsTypeAlias output = getFnOnceOutput();
        if (output == null) return null;

        TyInfer.TyVar inputArgVar = new TyInfer.TyVar();
        for (RsTraitItem fnTrait : fnTraits) {
            Pair<RsTraitItem, RsTypeAlias> pair = new Pair<>(fnTrait, output);
            SelectionResult<TyWithObligations<Ty>> projResult = selectProjection(pair, ty, inputArgVar);
            TyWithObligations<Ty> ok = projResult.ok();
            if (ok != null) {
                Ty resolved = getCtx().shallowResolve(inputArgVar);
                List<Ty> paramTypes = resolved instanceof TyTuple ? ((TyTuple) resolved).getTypes() : Collections.emptyList();
                TyFunctionPointer fnPtr = new TyFunctionPointer(new FnSig(paramTypes, ok.getValue(), Unsafety.Normal));
                return new TyWithObligations<>(fnPtr, ok.getObligations());
            }
        }
        return null;
    }

    @Nullable
    public TyFunctionBase asTyFunction(@NotNull BoundElement<RsTraitItem> ref) {
        RsTypeAlias outputParam = getFnOnceOutput();
        if (outputParam == null) return null;
        TyTypeParameter param = getTypeParamSingle(ref.getTypedElement());
        if (param == null) return null;
        Ty argTy = ref.getSubst().get(param);
        List<Ty> argumentTypes = (argTy instanceof TyTuple) ? ((TyTuple) argTy).getTypes() : Collections.emptyList();
        Ty outputType = ref.getAssoc().getOrDefault(outputParam, TyUnit.INSTANCE);
        return new TyFunctionPointer(new FnSig(argumentTypes, outputType));
    }

    @NotNull
    public TyWithObligations<Ty> lookupFutureOutputTy(@NotNull Ty ty, boolean strict) {
        RsTraitItem futureTrait = items.getIntoFuture();
        if (futureTrait == null) futureTrait = items.getFuture();
        if (futureTrait == null) return new TyWithObligations<>(TyUnknown.INSTANCE);
        RsTypeAlias outputType = RsTraitItemUtil.findAssociatedType(futureTrait, "Output");
        if (outputType == null) return new TyWithObligations<>(TyUnknown.INSTANCE);
        BoundElement<RsTypeAlias> outputBound = new BoundElement<>(outputType).withSubst();
        TraitRef traitRef = new TraitRef(ty, new BoundElement<>(futureTrait).withSubst());
        SelectionResult<TyWithObligations<Ty>> selection = strict
            ? selectProjectionStrict(traitRef, outputBound)
            : selectProjection(traitRef, outputBound);
        TyWithObligations<Ty> result = selection.ok();
        return result != null ? result : new TyWithObligations<>(TyUnknown.INSTANCE);
    }

    public boolean isSized(@NotNull Ty ty) {
        return isTraitImplemented(ty, items.getSized()) != ThreeValuedLogic.False;
    }

    @NotNull
    public ThreeValuedLogic isDeref(@NotNull Ty ty) { return isTraitImplemented(ty, items.getDeref()); }
    @NotNull
    public ThreeValuedLogic isCopy(@NotNull Ty ty) { return isTraitImplemented(ty, items.getCopy()); }
    @NotNull
    public ThreeValuedLogic isClone(@NotNull Ty ty) { return isTraitImplemented(ty, items.getClone()); }
    @NotNull
    public ThreeValuedLogic isDebug(@NotNull Ty ty) { return isTraitImplemented(ty, items.getDebug()); }
    @NotNull
    public ThreeValuedLogic isDefault(@NotNull Ty ty) { return isTraitImplemented(ty, items.getDefault()); }
    @NotNull
    public ThreeValuedLogic isEq(@NotNull Ty ty) { return isTraitImplemented(ty, items.getEq()); }
    @NotNull
    public ThreeValuedLogic isPartialEq(@NotNull Ty ty, @NotNull Ty rhsType) { return isTraitImplemented(ty, items.getPartialEq(), rhsType); }
    @NotNull
    public ThreeValuedLogic isPartialEq(@NotNull Ty ty) { return isPartialEq(ty, ty); }
    @NotNull
    public ThreeValuedLogic isIntoIterator(@NotNull Ty ty) { return isTraitImplemented(ty, items.getIntoIterator()); }
    @NotNull
    public ThreeValuedLogic isDrop(@NotNull Ty ty) { return isTraitImplemented(ty, items.getDrop()); }
    @NotNull
    public ThreeValuedLogic isIndex(@NotNull Ty ty, @NotNull Ty indexType) { return isTraitImplemented(ty, items.getIndex(), indexType); }

    @NotNull
    private ThreeValuedLogic isTraitImplemented(@NotNull Ty ty, @Nullable RsTraitItem trait, @NotNull Ty... subst) {
        if (trait == null) return ThreeValuedLogic.Unknown;
        return ThreeValuedLogic.fromBoolean(canSelect(new TraitRef(ty, new BoundElement<>(trait).withSubst(subst))));
    }

    @Nullable
    private static TyTypeParameter getTypeParamSingle(@NotNull RsTraitItem trait) {
        List<RsTypeParameter> params = trait.getTypeParameters();
        if (params.size() != 1) return null;
        return TyTypeParameter.named(params.get(0));
    }

    @NotNull
    public static ImplLookup relativeTo(@NotNull RsElement psi) {
        RsItemElement parentItem = PsiElementUtil.contextOrSelf(psi, RsItemElement.class);
        ParamEnv paramEnvResult;
        if (parentItem instanceof RsGenericDeclaration) {
            // Simplified: build param env for the parent item
            paramEnvResult = ParamEnv.buildFor(parentItem);
        } else if (parentItem != null) {
            paramEnvResult = ParamEnv.buildFor(parentItem);
        } else {
            paramEnvResult = ParamEnv.EMPTY;
        }
        return new ImplLookup(psi.getProject(), psi.getContainingCrate(), KnownItems.getKnownItems(psi), paramEnvResult, psi, TypeInferenceOptions.DEFAULT);
    }

    // ---- Helper classes ----

    @NotNull
    private static Map<TyFingerprint, List<RsCachedImplItem>> doGetImplsFromNestedMacros(@NotNull RsElement element) {
        // Simplified: return empty map
        return Collections.emptyMap();
    }

    private interface ImplsFilter {
        boolean canProcessImpl(@NotNull RsCachedImplItem impl);

        final class AllowAll implements ImplsFilter {
            public static final AllowAll INSTANCE = new AllowAll();
            private AllowAll() {}

            @Override
            public boolean canProcessImpl(@NotNull RsCachedImplItem impl) {
                return true;
            }
        }

        final class ConstBodyInsideImplSignatureFilter implements ImplsFilter {
            @NotNull
            private final Crate containingCrate;
            private final boolean allowInherentImpls;

            public ConstBodyInsideImplSignatureFilter(@NotNull Crate containingCrate, boolean allowInherentImpls) {
                this.containingCrate = containingCrate;
                this.allowInherentImpls = allowInherentImpls;
            }

            @Override
            public boolean canProcessImpl(@NotNull RsCachedImplItem impl) {
                return (allowInherentImpls && impl.isInherent())
                    || !impl.getContainingCrates().contains(containingCrate);
            }
        }
    }

    /**
     * Simple helper pair class.
     */
    public static class Pair<A, B> {
        @NotNull private final A first;
        @NotNull private final B second;

        public Pair(@NotNull A first, @NotNull B second) {
            this.first = first;
            this.second = second;
        }

        @NotNull
        public A getFirst() { return first; }
        @NotNull
        public B getSecond() { return second; }
    }

    @FunctionalInterface
    public interface RsProcessor<T> {
        boolean process(@NotNull T t);
    }

    public interface Sequence<T> extends Iterable<T> {
    }
}
