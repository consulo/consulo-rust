/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.lang.core.resolve;

import org.rust.stdext.Lazy;
import consulo.project.Project;
import consulo.util.dataholder.Key;
import consulo.application.util.CachedValue;
import consulo.application.util.CachedValueProvider;
import consulo.application.util.CachedValuesManager;
import consulo.language.psi.PsiModificationTracker;
import consulo.util.collection.SmartList;
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
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
import consulo.language.psi.PsiElement;
import consulo.language.psi.PsiFile;
import org.rust.lang.core.psi.RsBound;
import org.rust.lang.core.psi.RsPath;
import org.rust.lang.core.psi.RsTraitRef;
import org.rust.lang.core.psi.RsTypeReference;
import org.rust.lang.core.psi.RsWherePred;
import org.rust.lang.core.psi.ext.RsInferenceContextOwner;
import org.rust.lang.core.types.ExtensionsUtil;

public class ImplLookup {

    public static final int DEFAULT_RECURSION_LIMIT = 128;

    @Nonnull
    private final Project project;
    @Nonnull
    private final Crate containingCrate;
    @Nonnull
    private final KnownItems items;
    @Nonnull
    private final ParamEnv paramEnv;
    @Nullable
    private final RsElement context;
    @Nonnull
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
        @Nonnull Project project,
        @Nonnull Crate containingCrate,
        @Nonnull KnownItems items,
        @Nonnull ParamEnv paramEnv
    ) {
        this(project, containingCrate, items, paramEnv, null, TypeInferenceOptions.DEFAULT);
    }

    public ImplLookup(
        @Nonnull Project project,
        @Nonnull Crate containingCrate,
        @Nonnull KnownItems items,
        @Nonnull ParamEnv paramEnv,
        @Nullable RsElement context,
        @Nonnull TypeInferenceOptions options
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

    @Nonnull
    public KnownItems getItems() {
        return items;
    }

    @Nonnull
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

    @Nonnull
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

    @Nonnull
    private ImplsFilter getImplsFilter() {
        if (!implsFilterInitialized) {
            implsFilter = computeImplsFilter();
            implsFilterInitialized = true;
        }
        return implsFilter;
    }

    /**
     * Returns a filter that hides the impls of the current crate while inferring a const argument that is
     * part of an impl's own signature, as in
     *
     * <pre>{@code
     * impl Foo< {0} > for Bar< {0} > {}
     * //        ~~~            ~~~
     * }</pre>
     *
     * Resolving such a const against those same impls would recurse back into this inference.
     */
    @Nonnull
    private ImplsFilter computeImplsFilter() {
        if (context == null) return ImplsFilter.AllowAll.INSTANCE;
        RsInferenceContextOwner owner =
            ExtensionsUtil.getInferenceContextOwner(context);
        if (!(owner instanceof RsPath)) return ImplsFilter.AllowAll.INSTANCE;

        RsImplItem ancestorImpl = null;
        PsiElement previous = null;
        for (PsiElement current = owner.getContext(); current != null; current = current.getContext()) {
            if (current instanceof RsImplItem
                && (previous instanceof RsTraitRef
                || previous == ((RsImplItem) current).getTypeReference())) {
                ancestorImpl = (RsImplItem) current;
                break;
            }
            previous = current;
        }
        if (ancestorImpl == null) return ImplsFilter.AllowAll.INSTANCE;

        boolean isInsideTraitImpl = ancestorImpl.getTraitRef() != null;
        return new ImplsFilter.ConstBodyInsideImplSignatureFilter(containingCrate, isInsideTraitImpl);
    }

    @Nonnull
    public ParamEnv.Sequence<BoundElement<RsTraitItem>> getEnvBoundTransitivelyFor(@Nonnull Ty ty) {
        return paramEnv.boundsFor(ty);
    }

    /**
     * Resulting sequence is ordered: inherent impls are placed to the head.
     */
    @Nonnull
    public List<TraitImplSource> findImplsAndTraits(@Nonnull Ty ty) {
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

    @Nonnull
    private List<TraitImplSource> rawFindImplsAndTraits(@Nonnull Ty ty) {
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

    @Nonnull
    private Collection<RsTraitItem> findDerivedTraits(@Nonnull Ty ty) {
        if (ty instanceof TyAdt) {
            Collection<RsTraitItem> derived = RsStructOrEnumItemElementUtil.getDerivedTraits(((TyAdt) ty).getItem());
            if (derived == null) return Collections.emptyList();
            return derived.stream()
                .filter(RsTraitItemImplUtil::isKnownDerivable)
                .collect(Collectors.toList());
        }
        return Collections.emptyList();
    }

    private boolean findExplicitImpls(@Nonnull Ty selfTy, @Nonnull RsProcessor<RsCachedImplItem> processor) {
        return processTyFingerprintsWithAliases(selfTy, tyFingerprint ->
            findExplicitImplsWithoutAliases(selfTy, tyFingerprint, processor));
    }

    private boolean findExplicitImplsWithoutAliases(
        @Nonnull Ty selfTy,
        @Nonnull TyFingerprint tyf,
        @Nonnull RsProcessor<RsCachedImplItem> processor
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

    private boolean processTyFingerprintsWithAliases(@Nonnull Ty selfTy, @Nonnull RsProcessor<TyFingerprint> processor) {
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

    @Nonnull
    private List<RsCachedImplItem> findPotentialImpls(@Nonnull TyFingerprint tyf) {
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

    @Nonnull
    private List<String> findPotentialAliases(@Nonnull TyFingerprint tyf) {
        return indexCache.findPotentialAliases(tyf);
    }

    private boolean useImplsFromCrate(@Nonnull List<Crate> crates) {
        for (Crate c : crates) {
            if (Crate.hasTransitiveDependencyOrSelf(containingCrate, c)) return true;
        }
        return false;
    }

    private boolean canCombineTypes(
        @Nonnull Ty ty1,
        @Nonnull Ty ty2,
        @Nonnull List<TyTypeParameter> genericsForTy2,
        @Nonnull List<CtConstParameter> constGenericsForTy2
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

    @Nonnull
    private List<RsCachedImplItem> findBlanketImpls() {
        return findPotentialImpls(TyFingerprint.TYPE_PARAMETER_OR_MACRO_FINGERPRINT);
    }

    public boolean canSelectWithDeref(@Nonnull TraitRef ref) {
        return canSelectWithDeref(ref, 0);
    }

    public boolean canSelectWithDeref(@Nonnull TraitRef ref, int recursionDepth) {
        for (Ty ty : coercionSequence(ref.getSelfTy())) {
            if (canSelect(new TraitRef(ty, ref.getTrait()), recursionDepth)) return true;
        }
        return false;
    }

    public boolean canSelect(@Nonnull TraitRef ref) {
        return canSelect(ref, 0);
    }

    public boolean canSelect(@Nonnull TraitRef ref, int recursionDepth) {
        return selectStrictWithoutConfirm(ref, recursionDepth).isOk();
    }

    @Nonnull
    public SelectionResult<Selection> selectStrict(@Nonnull TraitRef ref) {
        return selectStrict(ref, 0);
    }

    @Nonnull
    public SelectionResult<Selection> selectStrict(@Nonnull TraitRef ref, int recursionDepth) {
        SelectionResult<SelectionCandidate> result = selectStrictWithoutConfirm(ref, recursionDepth);
        return result.andThen(candidate -> confirmCandidate(ref, candidate, recursionDepth));
    }

    @Nonnull
    private SelectionResult<SelectionCandidate> selectStrictWithoutConfirm(@Nonnull TraitRef ref, int recursionDepth) {
        SelectionResult<SelectionCandidate> result = selectWithoutConfirm(ref, BoundConstness.NotConst, recursionDepth);
        SelectionCandidate candidate = result.ok();
        if (candidate == null) return result.map(c -> { throw new IllegalStateException("unreachable"); });
        if (!canEvaluateObligations(ref, candidate, recursionDepth)) return SelectionResult.err();
        return result;
    }

    @Nonnull
    public SelectionResult<Selection> select(@Nonnull TraitRef ref) {
        return select(ref, 0);
    }

    @Nonnull
    public SelectionResult<Selection> select(@Nonnull TraitRef ref, int recursionDepth) {
        return select(ref, BoundConstness.NotConst, recursionDepth);
    }

    @Nonnull
    public SelectionResult<Selection> select(@Nonnull TraitRef ref, @Nonnull BoundConstness constness, int recursionDepth) {
        return selectWithoutConfirm(ref, constness, recursionDepth)
            .andThen(candidate -> confirmCandidate(ref, candidate, recursionDepth));
    }

    @Nonnull
    private SelectionResult<SelectionCandidate> selectWithoutConfirm(
        @Nonnull TraitRef ref,
        @Nonnull BoundConstness constness,
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

    @Nonnull
    private SelectionResult<SelectionCandidate> selectCandidate(@Nonnull TraitRef ref, int recursionDepth) {
        if (ref.getSelfTy() instanceof TyInfer.TyVar) {
            return SelectionResult.ambiguous();
        }
        if (ref.getSelfTy() instanceof TyReference && ((TyReference) ref.getSelfTy()).getReferenced() instanceof TyInfer.TyVar) {
            // TyFingerprint is never created for a type variable, and a reference is the one case
            // that unwraps while the fingerprint is built.
            return SelectionResult.ambiguous();
        }

        List<SelectionCandidate> assembled = assembleCandidates(ref);

        List<SelectionCandidate> candidates = new ArrayList<>();
        for (SelectionCandidate candidate : assembled) {
            if (candidate instanceof SelectionCandidate.ImplCandidate.ExplicitImpl
                && ((SelectionCandidate.ImplCandidate.ExplicitImpl) candidate).isNegativeImpl()) {
                continue;
            }
            candidates.add(candidate);
        }

        if (candidates.isEmpty()) return SelectionResult.err();
        if (candidates.size() == 1) return SelectionResult.ok(candidates.get(0));

        // More than one impl applies: keep only those whose own bounds can actually be satisfied.
        List<SelectionCandidate> filtered = new ArrayList<>();
        for (SelectionCandidate candidate : candidates) {
            if (canEvaluateObligations(ref, candidate, recursionDepth)) {
                filtered.add(candidate);
            }
        }

        if (filtered.isEmpty()) return SelectionResult.err();
        if (filtered.size() == 1) return SelectionResult.ok(filtered.get(0));

        int i = 0;
        while (i < filtered.size()) {
            boolean isDup = false;
            for (int j = 0; j < filtered.size(); j++) {
                if (j == i) continue;
                if (candidateShouldBeDroppedInFavorOf(filtered.get(i), filtered.get(j))) {
                    isDup = true;
                    break;
                }
            }
            if (isDup) {
                filtered.set(i, filtered.get(filtered.size() - 1));
                filtered.remove(filtered.size() - 1);
            }
            else {
                i++;
                if (i > 1) return SelectionResult.ambiguous();
            }
        }
        return filtered.size() == 1 ? SelectionResult.ok(filtered.get(0)) : SelectionResult.ambiguous();
    }

    /** An impl written for a concrete type wins over a blanket impl written for a type parameter. */
    private boolean candidateShouldBeDroppedInFavorOf(
        @Nonnull SelectionCandidate victim,
        @Nonnull SelectionCandidate other
    ) {
        if (!(victim instanceof SelectionCandidate.ImplCandidate.ExplicitImpl)
            || !(other instanceof SelectionCandidate.ImplCandidate.ExplicitImpl)) {
            return false;
        }
        SelectionCandidate.ImplCandidate.ExplicitImpl v = (SelectionCandidate.ImplCandidate.ExplicitImpl) victim;
        SelectionCandidate.ImplCandidate.ExplicitImpl o = (SelectionCandidate.ImplCandidate.ExplicitImpl) other;
        if (v.getImpl() == o.getImpl()) return false;
        return v.getFormalSelfTy() instanceof TyTypeParameter
            && !(o.getFormalSelfTy() instanceof TyTypeParameter);
    }

    @Nonnull
    private List<SelectionCandidate> assembleCandidates(@Nonnull TraitRef ref) {
        List<SelectionCandidate> candidates = new ArrayList<>();
        assembleCandidatesFromImpls(ref, candidates);
        assembleCandidatesFromCallerBounds(ref, candidates);
        return candidates;
    }

    private void assembleCandidatesFromImpls(@Nonnull TraitRef ref, @Nonnull List<SelectionCandidate> candidates) {
        processTyFingerprintsWithAliases(ref.getSelfTy(), tyFingerprint -> {
            for (RsCachedImplItem cachedImpl : findPotentialImpls(tyFingerprint)) {
                SelectionCandidate candidate = trySelectCandidate(cachedImpl, ref);
                if (candidate != null) candidates.add(candidate);
            }
            return false;
        });
    }

    /** The impl applies if its own trait reference can be unified with the one being selected. */
    @Nullable
    private SelectionCandidate trySelectCandidate(@Nonnull RsCachedImplItem cachedImpl, @Nonnull TraitRef ref) {
        BoundElement<RsTraitItem> formalTraitRef = cachedImpl.getImplementedTrait();
        if (formalTraitRef == null) return null;
        if (formalTraitRef.getTypedElement() != ref.getTrait().getTypedElement()) return null;

        Ty formalSelfTy = cachedImpl.getType();
        List<TyTypeParameter> generics = cachedImpl.getGenerics();
        List<CtConstParameter> constGenerics = cachedImpl.getConstGenerics();
        if (formalSelfTy == null || generics == null || constGenerics == null) return null;

        boolean probe = getCtx().probe(() -> {
            SelectionCandidate.Triple<Substitution, TraitRef, List<Obligation>> prepared =
                ImplLookupUtil.prepareSubstAndTraitRefRaw(
                    getCtx(), generics, constGenerics, formalSelfTy, formalTraitRef, 0);
            return getCtx().combineTraitRefs(prepared.getSecond(), ref);
        });
        if (!probe) return null;

        return new SelectionCandidate.ImplCandidate.ExplicitImpl(
            cachedImpl.getImpl(), formalSelfTy, formalTraitRef, cachedImpl.isNegativeImpl());
    }

    private void assembleCandidatesFromCallerBounds(
        @Nonnull TraitRef ref,
        @Nonnull List<SelectionCandidate> candidates
    ) {
        for (BoundElement<RsTraitItem> bound : getEnvBoundTransitivelyFor(ref.getSelfTy())) {
            if (getCtx().probe(() -> getCtx().combineBoundElements(bound, ref.getTrait()))) {
                candidates.add(new SelectionCandidate.ParamCandidate(bound));
            }
        }
    }

    /**
     * Canonicalises the inference variables in a cache key: two selections that differ only in which
     * variables they mention are the same query.
     */
    private <T extends TypeFoldable<T>> T freshen(@Nonnull T ty) {
        return FoldUtil.foldTyInferWithTyPlaceholder(ty);
    }

    private boolean canEvaluateObligations(@Nonnull TraitRef ref, @Nonnull SelectionCandidate candidate, int recursionDepth) {
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

    @Nonnull
    private SelectionResult<Selection> confirmCandidate(
        @Nonnull TraitRef ref,
        @Nonnull SelectionCandidate candidate,
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

    @Nonnull
    private Selection confirmParamCandidate(@Nonnull TraitRef ref, @Nonnull SelectionCandidate.ParamCandidate candidate) {
        getCtx().combineBoundElements(candidate.getBound(), ref.getTrait());
        return new Selection(candidate.getBound().getTypedElement(), Collections.emptyList());
    }

    @Nonnull
    private Selection confirmImplCandidate(
        @Nonnull TraitRef ref,
        @Nonnull SelectionCandidate.ImplCandidate candidate,
        int recursionDepth
    ) {
        if (candidate instanceof SelectionCandidate.ImplCandidate.DerivedTrait) {
            return confirmDerivedCandidate(ref, (SelectionCandidate.ImplCandidate.DerivedTrait) candidate, recursionDepth);
        }
        if (candidate instanceof SelectionCandidate.ImplCandidate.ExplicitImpl) {
            SelectionCandidate.ImplCandidate.ExplicitImpl explicit =
                (SelectionCandidate.ImplCandidate.ExplicitImpl) candidate;
            SelectionCandidate.Triple<Substitution, TraitRef, List<Obligation>> prepared =
                explicit.prepareSubstAndTraitRef(getCtx(), recursionDepth + 1);
            getCtx().combineTraitRefs(ref, prepared.getSecond());

            // Resolve the variables now so an already-inferred obligation caches under a stable key.
            Map<TyTypeParameter, Ty> selfMap = new HashMap<>();
            selfMap.put(TyTypeParameter.self(), ref.getSelfTy());
            Substitution candidateSubst = getCtx().resolveTypeVarsIfPossible(prepared.getFirst())
                .plus(SubstitutionUtil.toTypeSubst(selfMap));

            List<Obligation> obligations = new ArrayList<>(prepared.getThird());
            for (Obligation o : getCtx().instantiateBounds(
                org.rust.lang.core.psi.ext.RsGenericDeclarationUtil.getPredicates(explicit.getImpl()),
                candidateSubst, recursionDepth + 1)) {
                obligations.add(o);
            }
            return new Selection(explicit.getImpl(), obligations, candidateSubst);
        }
        return new Selection(ref.getTrait().getTypedElement(), Collections.emptyList());
    }

    @Nonnull
    private Selection confirmDerivedCandidate(
        @Nonnull TraitRef ref,
        @Nonnull SelectionCandidate.ImplCandidate.DerivedTrait candidate,
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

    @Nonnull
    public Autoderef coercionSequence(@Nonnull Ty baseTy) {
        return new Autoderef(this, getCtx(), baseTy);
    }

    @Nullable
    public TyWithObligations<Ty> deref(@Nonnull Ty ty) {
        if (ty instanceof TyReference) {
            return new TyWithObligations<>(((TyReference) ty).getReferenced());
        }
        if (ty instanceof TyPointer) {
            return new TyWithObligations<>(((TyPointer) ty).getReferenced());
        }
        return findDerefTarget(ty);
    }

    @Nullable
    private TyWithObligations<Ty> findDerefTarget(@Nonnull Ty ty) {
        if (derefTraitAndTarget == null) return null;
        SelectionResult<TyWithObligations<Ty>> result = selectProjection(derefTraitAndTarget, ty);
        return result.ok();
    }

    @Nullable
    public TyWithObligations<Ty> findIteratorItemType(@Nonnull Ty ty) {
        Pair<RsTraitItem, RsTypeAlias> pair = getIntoIteratorTraitAndOutput();
        if (pair == null) return null;
        SelectionResult<TyWithObligations<Ty>> result = selectProjection(pair, ty);
        return result.ok();
    }

    @Nullable
    public TyWithObligations<Ty> findIndexOutputType(@Nonnull Ty containerType, @Nonnull Ty indexType) {
        Pair<RsTraitItem, RsTypeAlias> pair = getIndexTraitAndOutput();
        if (pair == null) return null;
        SelectionResult<TyWithObligations<Ty>> result = selectProjection(pair, containerType, indexType);
        return result.ok();
    }

    @Nullable
    public TyWithObligations<Ty> findArithmeticBinaryExprOutputType(@Nonnull Ty lhsType, @Nonnull Ty rhsType, @Nonnull ArithmeticOp op) {
        RsTraitItem trait = op.findTrait(items);
        if (trait == null) return null;
        RsTypeAlias assocType = RsTraitItemUtil.findAssociatedType(trait, "Output");
        if (assocType == null) return null;
        TyProjection projection = TyProjection.valueOf(lhsType, new BoundElement<>(assocType).withSubst(rhsType));
        return getCtx().normalizeAssociatedTypesIn(projection);
    }

    @Nonnull
    private SelectionResult<TyWithObligations<Ty>> selectProjection(
        @Nonnull Pair<RsTraitItem, RsTypeAlias> traitAndOutput,
        @Nonnull Ty selfTy,
        @Nonnull Ty... subst
    ) {
        TraitRef ref = new TraitRef(selfTy, new BoundElement<>(traitAndOutput.getFirst()).withSubst(subst));
        return selectProjection(ref, new BoundElement<>(traitAndOutput.getSecond()).withSubst());
    }

    @Nonnull
    public SelectionResult<TyWithObligations<Ty>> selectProjection(
        @Nonnull TraitRef ref,
        @Nonnull BoundElement<RsTypeAlias> assocType
    ) {
        return selectProjection(ref, assocType, 0);
    }

    @Nonnull
    public SelectionResult<TyWithObligations<Ty>> selectProjection(
        @Nonnull TraitRef ref,
        @Nonnull BoundElement<RsTypeAlias> assocType,
        int recursionDepth
    ) {
        return select(ref, recursionDepth).map(selection -> {
            Ty looked = lookupAssociatedType(ref.getSelfTy(), selection, assocType);
            if (looked == null) return null;
            TyWithObligations<Ty> normalized = getCtx().normalizeAssociatedTypesIn(looked, recursionDepth + 1);
            return normalized.withObligations(selection.getNestedObligations());
        });
    }

    @Nonnull
    public SelectionResult<TyWithObligations<Ty>> selectProjection(
        @Nonnull TyProjection projectionTy
    ) {
        return selectProjection(projectionTy, 0);
    }

    @Nonnull
    public SelectionResult<TyWithObligations<Ty>> selectProjection(
        @Nonnull TyProjection projectionTy,
        int recursionDepth
    ) {
        return selectProjection(projectionTy.getTraitRef(), projectionTy.getTarget(), recursionDepth);
    }

    @Nonnull
    public SelectionResult<TyWithObligations<Ty>> selectProjectionStrict(
        @Nonnull TraitRef ref,
        @Nonnull BoundElement<RsTypeAlias> assocType
    ) {
        return selectProjectionStrict(ref, assocType, 0);
    }

    @Nonnull
    public SelectionResult<TyWithObligations<Ty>> selectProjectionStrict(
        @Nonnull TraitRef ref,
        @Nonnull BoundElement<RsTypeAlias> assocType,
        int recursionDepth
    ) {
        return selectStrict(ref, recursionDepth).map(selection -> {
            Ty looked = lookupAssociatedType(ref.getSelfTy(), selection, assocType);
            if (looked == null) return null;
            TyWithObligations<Ty> normalized = getCtx().normalizeAssociatedTypesIn(looked, recursionDepth + 1);
            return normalized.withObligations(selection.getNestedObligations());
        });
    }

    @Nonnull
    public SelectionResult<TyWithObligations<Ty>> selectProjectionStrictWithDeref(
        @Nonnull TraitRef ref,
        @Nonnull BoundElement<RsTypeAlias> assocType
    ) {
        return selectProjectionStrictWithDeref(ref, assocType, 0);
    }

    @Nonnull
    public SelectionResult<TyWithObligations<Ty>> selectProjectionStrictWithDeref(
        @Nonnull TraitRef ref,
        @Nonnull BoundElement<RsTypeAlias> assocType,
        int recursionDepth
    ) {
        for (Ty ty : coercionSequence(ref.getSelfTy())) {
            SelectionResult<TyWithObligations<Ty>> result = selectProjectionStrict(new TraitRef(ty, ref.getTrait()), assocType, recursionDepth);
            if (result.isOk()) return result;
        }
        return SelectionResult.err();
    }

    @Nullable
    public Map<RsTypeAlias, Ty> selectAllProjectionsStrict(@Nonnull TraitRef ref) {
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
    private Ty lookupAssociatedType(@Nonnull Ty selfTy, @Nonnull Selection res, @Nonnull BoundElement<RsTypeAlias> assocType) {
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
    private Ty lookupAssocTypeInSelection(@Nonnull Selection selection, @Nonnull BoundElement<RsTypeAlias> assocDef) {
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
        @Nonnull Iterable<BoundElement<RsTraitItem>> subst,
        @Nonnull RsTraitOrImpl trait,
        @Nonnull BoundElement<RsTypeAlias> assocType
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
    public RsTraitOrImpl findOverloadedOpImpl(@Nonnull Ty lhsType, @Nonnull Ty rhsType, @Nonnull OverloadableBinaryOperator op) {
        RsTraitItem trait = op.findTrait(items);
        if (trait == null) return null;
        Selection selection = select(new TraitRef(lhsType, new BoundElement<>(trait).withSubst(rhsType))).ok();
        return selection != null ? selection.getImpl() : null;
    }

    @Nullable
    public TyWithObligations<TyFunctionBase> asTyFunction(@Nonnull Ty ty) {
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
    public TyFunctionBase asTyFunction(@Nonnull BoundElement<RsTraitItem> ref) {
        RsTypeAlias outputParam = getFnOnceOutput();
        if (outputParam == null) return null;
        TyTypeParameter param = getTypeParamSingle(ref.getTypedElement());
        if (param == null) return null;
        Ty argTy = ref.getSubst().get(param);
        List<Ty> argumentTypes = (argTy instanceof TyTuple) ? ((TyTuple) argTy).getTypes() : Collections.emptyList();
        Ty outputType = ref.getAssoc().getOrDefault(outputParam, TyUnit.INSTANCE);
        return new TyFunctionPointer(new FnSig(argumentTypes, outputType));
    }

    @Nonnull
    public TyWithObligations<Ty> lookupFutureOutputTy(@Nonnull Ty ty, boolean strict) {
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

    public boolean isSized(@Nonnull Ty ty) {
        return isTraitImplemented(ty, items.getSized()) != ThreeValuedLogic.False;
    }

    @Nonnull
    public ThreeValuedLogic isDeref(@Nonnull Ty ty) { return isTraitImplemented(ty, items.getDeref()); }
    @Nonnull
    public ThreeValuedLogic isCopy(@Nonnull Ty ty) { return isTraitImplemented(ty, items.getCopy()); }
    @Nonnull
    public ThreeValuedLogic isClone(@Nonnull Ty ty) { return isTraitImplemented(ty, items.getClone()); }
    @Nonnull
    public ThreeValuedLogic isDebug(@Nonnull Ty ty) { return isTraitImplemented(ty, items.getDebug()); }
    @Nonnull
    public ThreeValuedLogic isDefault(@Nonnull Ty ty) { return isTraitImplemented(ty, items.getDefault()); }
    @Nonnull
    public ThreeValuedLogic isEq(@Nonnull Ty ty) { return isTraitImplemented(ty, items.getEq()); }
    @Nonnull
    public ThreeValuedLogic isPartialEq(@Nonnull Ty ty, @Nonnull Ty rhsType) { return isTraitImplemented(ty, items.getPartialEq(), rhsType); }
    @Nonnull
    public ThreeValuedLogic isPartialEq(@Nonnull Ty ty) { return isPartialEq(ty, ty); }
    @Nonnull
    public ThreeValuedLogic isIntoIterator(@Nonnull Ty ty) { return isTraitImplemented(ty, items.getIntoIterator()); }
    @Nonnull
    public ThreeValuedLogic isDrop(@Nonnull Ty ty) { return isTraitImplemented(ty, items.getDrop()); }
    @Nonnull
    public ThreeValuedLogic isIndex(@Nonnull Ty ty, @Nonnull Ty indexType) { return isTraitImplemented(ty, items.getIndex(), indexType); }

    @Nonnull
    private ThreeValuedLogic isTraitImplemented(@Nonnull Ty ty, @Nullable RsTraitItem trait, @Nonnull Ty... subst) {
        if (trait == null) return ThreeValuedLogic.Unknown;
        return ThreeValuedLogic.fromBoolean(canSelect(new TraitRef(ty, new BoundElement<>(trait).withSubst(subst))));
    }

    @Nullable
    private static TyTypeParameter getTypeParamSingle(@Nonnull RsTraitItem trait) {
        List<RsTypeParameter> params = trait.getTypeParameters();
        if (params.size() != 1) return null;
        return TyTypeParameter.named(params.get(0));
    }

    @Nonnull
    public static ImplLookup relativeTo(@Nonnull RsElement psi) {
        RsItemElement parentItem = PsiElementUtil.contextOrSelf(psi, RsItemElement.class);
        ParamEnv paramEnvResult;
        if (parentItem instanceof RsGenericDeclaration) {
            PsiElement ancestor = null;
            PsiElement cameFrom = null;
            // Walk `psi` and its context chain, stopping at the containing file; `cameFrom` is the
            // element of that chain directly below the first where-predicate, bound or impl found.
            PsiElement previous = null;
            for (PsiElement current = psi;
                 current != null;
                 current = current instanceof PsiFile ? null : current.getContext()) {
                if (current instanceof RsWherePred
                    || current instanceof RsBound
                    || current instanceof RsImplItem) {
                    ancestor = current;
                    cameFrom = previous;
                    break;
                }
                previous = current;
            }
            boolean isInTraitBoundOrImplSignature = ancestor != null
                && (!(ancestor instanceof RsImplItem)
                || cameFrom instanceof RsTraitRef
                || cameFrom instanceof RsTypeReference);
            // Inside a bound or an impl signature the type parameter bounds have to be computed lazily,
            // because computing them eagerly resolves back into the very bound being processed.
            paramEnvResult = isInTraitBoundOrImplSignature
                ? new LazyParamEnv((RsGenericDeclaration) parentItem)
                : ParamEnv.buildFor(parentItem);
        } else if (parentItem != null) {
            paramEnvResult = ParamEnv.buildFor(parentItem);
        } else {
            paramEnvResult = ParamEnv.EMPTY;
        }
        return new ImplLookup(psi.getProject(), psi.getContainingCrate(), KnownItems.getKnownItems(psi), paramEnvResult, psi, TypeInferenceOptions.DEFAULT);
    }

    // ---- Helper classes ----

    @Nonnull
    private static Map<TyFingerprint, List<RsCachedImplItem>> doGetImplsFromNestedMacros(@Nonnull RsElement element) {
        // Simplified: return empty map
        return Collections.emptyMap();
    }

    private interface ImplsFilter {
        boolean canProcessImpl(@Nonnull RsCachedImplItem impl);

        final class AllowAll implements ImplsFilter {
            public static final AllowAll INSTANCE = new AllowAll();
            private AllowAll() {}

            @Override
            public boolean canProcessImpl(@Nonnull RsCachedImplItem impl) {
                return true;
            }
        }

        final class ConstBodyInsideImplSignatureFilter implements ImplsFilter {
            @Nonnull
            private final Crate containingCrate;
            private final boolean allowInherentImpls;

            public ConstBodyInsideImplSignatureFilter(@Nonnull Crate containingCrate, boolean allowInherentImpls) {
                this.containingCrate = containingCrate;
                this.allowInherentImpls = allowInherentImpls;
            }

            @Override
            public boolean canProcessImpl(@Nonnull RsCachedImplItem impl) {
                return (allowInherentImpls && impl.isInherent())
                    || !impl.getContainingCrates().contains(containingCrate);
            }
        }
    }

    /**
     * Simple helper pair class.
     */
    public static class Pair<A, B> {
        @Nonnull private final A first;
        @Nonnull private final B second;

        public Pair(@Nonnull A first, @Nonnull B second) {
            this.first = first;
            this.second = second;
        }

        @Nonnull
        public A getFirst() { return first; }
        @Nonnull
        public B getSecond() { return second; }
    }

    @FunctionalInterface
    public interface RsProcessor<T> {
        boolean process(@Nonnull T t);
    }

    public interface Sequence<T> extends Iterable<T> {
    }
}
