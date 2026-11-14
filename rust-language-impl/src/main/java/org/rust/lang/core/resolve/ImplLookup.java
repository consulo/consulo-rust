/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.lang.core.resolve;

import consulo.project.Project;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import org.rust.lang.core.crate.Crate;
import org.rust.lang.core.psi.*;
import org.rust.lang.core.psi.ext.*;
import org.rust.lang.core.types.*;
import org.rust.lang.core.types.consts.Const;
import org.rust.lang.core.types.consts.CtConstParameter;
import org.rust.lang.core.types.infer.*;
import org.rust.lang.core.types.infer.FoldUtil;
import org.rust.lang.core.types.ty.*;
import org.rust.lang.utils.evaluation.ThreeValuedLogic;

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
import org.rust.lang.core.psi.ext.impl.*;

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
            Set<RsTraitItem> seenObjectTraits = new HashSet<>();
            for (BoundElement<RsTraitItem> be : traitBoundsTransitively(((TyTraitObject) ty).getTraits())) {
                if (seenObjectTraits.add(be.getTypedElement())) {
                    implsAndTraits.add(new TraitImplSource.Object(be.getTypedElement()));
                }
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
            for (BoundElement<RsTraitItem> be : traitBoundsTransitively(((TyAnon) ty).getTraits())) {
                if (seen.add(be.getTypedElement())) {
                    implsAndTraits.add(new TraitImplSource.Object(be.getTypedElement()));
                }
            }
            for (RsCachedImplItem blanketImpl : findBlanketImpls()) {
                if (!blanketImpl.isNegativeImpl()) {
                    implsAndTraits.add(blanketImpl.getExplicitImpl());
                }
            }
        } else if (ty instanceof TyProjection projection) {
            Substitution subst = projectionSubst(projection);
            Set<RsTraitItem> seen = new LinkedHashSet<>();
            for (TraitRef bound : RsGenericDeclarationUtil.getBounds(projection.getTrait().getTypedElement())) {
                if (!getCtx().probe(() -> getCtx().combineTypes(FoldUtil.substitute(bound.getSelfTy(), subst), projection).isOk())) continue;
                for (BoundElement<RsTraitItem> be : RsTraitItemUtil.getFlattenHierarchy(bound.getTrait())) {
                    if (seen.add(be.getTypedElement())) implsAndTraits.add(new TraitImplSource.ProjectionBound(be.getTypedElement()));
                }
            }
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

    /**
     * The traits a type gets from its {@code #[derive(...)]} attributes.
     * <p>
     * Every trait here already resolved - either to one of the hardcoded std derivables or, for a
     * custom derive, to a trait of that name in scope at the derive - so the type really does
     * implement it and its members belong in lookup.
     */
    @Nonnull
    private Collection<RsTraitItem> findDerivedTraits(@Nonnull Ty ty) {
        if (ty instanceof TyAdt) {
            Collection<RsTraitItem> derived = RsStructOrEnumItemElementUtil.getDerivedTraits(((TyAdt) ty).getItem());
            if (derived == null) return Collections.emptyList();
            return derived;
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
            if (!useImplsFromCrate(impl.getContainingCrates())) continue;
            if (!getImplsFilter().canProcessImpl(impl)) continue;
            result.add(impl);
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

        CandidateSet candidateSet = assembleCandidates(ref);
        if (candidateSet.ambiguous) return SelectionResult.ambiguous();

        List<SelectionCandidate> candidates = new ArrayList<>();
        for (SelectionCandidate candidate : candidateSet.list) {
            if (candidate instanceof SelectionCandidate.ImplCandidate.ExplicitImpl explicit && explicit.isNegativeImpl()) continue;
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
                if (candidateShouldBeDroppedInFavorOf(ref.getSelfTy(), filtered.get(i), filtered.get(j))) {
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
                if (i > 1) return SelectionResult.ambiguous(filtered);
            }
        }
        return filtered.size() == 1 ? SelectionResult.ok(filtered.get(0)) : SelectionResult.ambiguous(filtered);
    }

    /** Mirrors rustc's {@code candidate_should_be_dropped_in_favor_of}. */
    private boolean candidateShouldBeDroppedInFavorOf(
        @Nonnull Ty selfTy,
        @Nonnull SelectionCandidate victim,
        @Nonnull SelectionCandidate other
    ) {
        if (victim.isEquivalentTo(other)) return true;

        if (other instanceof SelectionCandidate.BuiltinCandidate builtin && !builtin.hasNested()
            || other == SelectionCandidate.ConstDestructCandidate.INSTANCE) {
            return true;
        }
        if (victim instanceof SelectionCandidate.BuiltinCandidate builtin && !builtin.hasNested()
            || victim == SelectionCandidate.ConstDestructCandidate.INSTANCE) {
            return false;
        }
        if (other instanceof SelectionCandidate.ParamCandidate param && isNonParamCandidate(victim)) {
            boolean wins = !(FoldUtil.isGlobal(selfTy) && FoldUtil.isGlobal(param.getBound()));
            if (wins) TypeInferenceMarks.WinnowParamCandidateWins.hit();
            return wins;
        }
        if (isNonParamCandidate(other) && victim instanceof SelectionCandidate.ParamCandidate param) {
            return FoldUtil.isGlobal(selfTy) && FoldUtil.isGlobal(param.getBound());
        }
        if (isObjectOrProjection(other) && isNonParamCandidate(victim) && !isObjectOrProjection(victim)) {
            TypeInferenceMarks.WinnowObjectOrProjectionCandidateWins.hit();
            return true;
        }
        if (isNonParamCandidate(other) && !isObjectOrProjection(other) && isObjectOrProjection(victim)) {
            return false;
        }
        // basic specialization
        if (victim instanceof SelectionCandidate.ImplCandidate.ExplicitImpl v && v.getFormalSelfTy() instanceof TyTypeParameter
            && other instanceof SelectionCandidate.ImplCandidate.ExplicitImpl o && !(o.getFormalSelfTy() instanceof TyTypeParameter)) {
            TypeInferenceMarks.WinnowSpecialization.hit();
            return true;
        }
        return false;
    }

    private static boolean isNonParamCandidate(@Nonnull SelectionCandidate candidate) {
        return candidate instanceof SelectionCandidate.ImplCandidate
            || candidate instanceof SelectionCandidate.FnPointerCandidate
            || candidate instanceof SelectionCandidate.BuiltinUnsizeCandidate
            || candidate instanceof SelectionCandidate.BuiltinCandidate
            || isObjectOrProjection(candidate);
    }

    private static boolean isObjectOrProjection(@Nonnull SelectionCandidate candidate) {
        return candidate instanceof SelectionCandidate.ObjectCandidate
            || candidate instanceof SelectionCandidate.ProjectionCandidate;
    }

    private static final class CandidateSet {
        final List<SelectionCandidate> list = new ArrayList<>();
        boolean ambiguous;
    }

    /** The conditions under which a builtin impl of {@code Sized}, {@code Copy} or {@code Clone} applies. */
    private static final class BuiltinConditions {
        static final BuiltinConditions NONE = new BuiltinConditions(null);
        static final BuiltinConditions AMBIGUOUS = new BuiltinConditions(null);

        /** The types that must implement the trait too; {@code null} for {@link #NONE} and {@link #AMBIGUOUS}. */
        @Nullable final List<Ty> nested;

        private BuiltinConditions(@Nullable List<Ty> nested) {
            this.nested = nested;
        }

        static BuiltinConditions where(@Nonnull List<Ty> nested) {
            return new BuiltinConditions(nested);
        }
    }

    /** Mirrors rustc's {@code assemble_candidates}. */
    @Nonnull
    private CandidateSet assembleCandidates(@Nonnull TraitRef ref) {
        CandidateSet candidates = new CandidateSet();
        RsTraitItem trait = ref.getTrait().getTypedElement();
        if (trait == items.getCopy()) {
            assembleCandidatesFromImpls(ref, candidates.list);
            assembleBuiltinBoundCandidates(copyCloneConditions(ref.getSelfTy()), candidates);
        }
        else if (trait == items.getSized()) {
            assembleBuiltinBoundCandidates(sizedConditions(ref.getSelfTy()), candidates);
        }
        else if (trait == items.getUnsize()) {
            assembleCandidatesForUnsizing(ref, candidates);
        }
        else if (trait == items.getDestruct()) {
            candidates.list.add(SelectionCandidate.ConstDestructCandidate.INSTANCE);
        }
        else {
            if (trait == items.getClone()) {
                assembleBuiltinBoundCandidates(copyCloneConditions(ref.getSelfTy()), candidates);
            }
            assembleFnPointerCandidates(ref, candidates.list);
            assembleCandidatesFromImpls(ref, candidates.list);
            assembleCandidatesFromObjectTy(ref, candidates.list);
        }

        assembleCandidatesFromProjectedTys(ref, candidates.list);
        assembleCandidatesFromCallerBounds(ref, candidates.list);

        // Auto implementations have lower priority, so they are only considered when no other impl
        // applies. Note that the list also contains negative impls like `impl !Sync for Foo {}`
        if (candidates.list.isEmpty() && RsTraitItemUtil.isAuto(trait)) {
            // For now, any type is thought to be Sync + Send
            candidates.list.add(new SelectionCandidate.ParamCandidate(new BoundElement<>(trait)));
        }
        if (candidates.list.isEmpty() && !candidates.ambiguous) {
            assembleCopyCloneCandidates(ref, candidates.list);
        }
        return candidates;
    }

    private static void assembleBuiltinBoundCandidates(@Nonnull BuiltinConditions conditions, @Nonnull CandidateSet candidates) {
        if (conditions == BuiltinConditions.AMBIGUOUS) {
            candidates.ambiguous = true;
        }
        else if (conditions.nested != null) {
            candidates.list.add(new SelectionCandidate.BuiltinCandidate(!conditions.nested.isEmpty()));
        }
    }

    /** Mirrors rustc's {@code copy_clone_conditions}. */
    @Nonnull
    private static BuiltinConditions copyCloneConditions(@Nonnull Ty selfTy) {
        if (selfTy instanceof TyInfer.IntVar || selfTy instanceof TyInfer.FloatVar || selfTy instanceof TyFunctionBase
            || selfTy instanceof TyUnknown || selfTy instanceof TyUnit) {
            return BuiltinConditions.where(Collections.emptyList());
        }
        if (selfTy instanceof TyTuple tuple) return BuiltinConditions.where(tuple.getTypes());
        // Integers, floats, bool, char, pointers, `!`, references and arrays are implemented in libcore
        return BuiltinConditions.NONE;
    }

    @Nonnull
    private static BuiltinConditions sizedConditions(@Nonnull Ty selfTy) {
        if (selfTy instanceof TyInfer.IntVar
            || selfTy instanceof TyInfer.FloatVar
            || selfTy instanceof TyNumeric
            || selfTy instanceof TyBool
            || selfTy instanceof TyFunctionBase
            || selfTy instanceof TyPointer
            || selfTy instanceof TyReference
            || selfTy instanceof TyChar
            || selfTy instanceof TyArray
            || selfTy instanceof TyNever
            || selfTy instanceof TyUnit
            || selfTy instanceof TyUnknown) {
            return BuiltinConditions.where(Collections.emptyList());
        }
        if (selfTy instanceof TyTuple tuple) {
            List<Ty> types = tuple.getTypes();
            return BuiltinConditions.where(types.isEmpty() ? Collections.emptyList() : List.of(types.get(types.size() - 1)));
        }
        if (selfTy instanceof TyAdt) {
            Ty tail = TyUtil.structTail(selfTy);
            return BuiltinConditions.where(tail != null ? List.of(tail) : Collections.emptyList());
        }
        if (selfTy instanceof TyInfer.TyVar) return BuiltinConditions.AMBIGUOUS;
        // `str`, slices, trait objects, projections, type parameters and `impl Trait`
        return BuiltinConditions.NONE;
    }

    private void assembleFnPointerCandidates(@Nonnull TraitRef ref, @Nonnull List<SelectionCandidate> candidates) {
        RsTraitItem element = ref.getTrait().getTypedElement();
        for (BoundElement<RsTraitItem> be : getTyFunctionImpls(ref.getSelfTy())) {
            if (be.getTypedElement() == element && combinesWithTraitSubst(be, ref)) {
                candidates.add(SelectionCandidate.FnPointerCandidate.INSTANCE);
            }
        }
    }

    private boolean combinesWithTraitSubst(@Nonnull BoundElement<RsTraitItem> be, @Nonnull TraitRef ref) {
        return getCtx().probe(() ->
            getCtx().combineTypePairs(be.getSubst().zipTypeValues(ref.getTrait().getSubst())).isOk()
                && getCtx().combineConstPairs(be.getSubst().zipConstValues(ref.getTrait().getSubst())).isOk());
    }

    /** The `Fn*`, `Clone` and `Copy` impls every function type has. */
    @Nonnull
    private List<BoundElement<RsTraitItem>> getTyFunctionImpls(@Nonnull Ty ty) {
        if (!(ty instanceof TyFunctionBase function)) return Collections.emptyList();
        RsTypeAlias output = getFnOnceOutput();
        Ty args = function.getParamTypes().isEmpty() ? TyUnit.INSTANCE : new TyTuple(function.getParamTypes());
        Map<RsTypeAlias, Ty> assoc = output != null ? Map.of(output, function.getRetType()) : Collections.emptyMap();
        List<BoundElement<RsTraitItem>> result = new ArrayList<>();
        for (RsTraitItem fnTrait : fnTraits) {
            BoundElement<RsTraitItem> withArgs = new BoundElement<>(fnTrait).withSubst(args);
            result.add(new BoundElement<>(fnTrait, withArgs.getSubst(), assoc));
        }
        if (items.getClone() != null) result.add(new BoundElement<>(items.getClone()));
        if (items.getCopy() != null) result.add(new BoundElement<>(items.getCopy()));
        return result;
    }

    @Nonnull
    private static List<BoundElement<RsTraitItem>> traitBoundsTransitively(@Nonnull List<BoundElement<RsTraitItem>> traits) {
        List<BoundElement<RsTraitItem>> result = new ArrayList<>();
        for (BoundElement<RsTraitItem> trait : traits) {
            result.addAll(RsTraitItemUtil.getFlattenHierarchy(trait));
        }
        return result;
    }

    @Nullable
    private static BoundElement<RsTraitItem> findObjectBound(@Nonnull Ty selfTy, @Nonnull RsTraitItem trait) {
        List<BoundElement<RsTraitItem>> traits;
        if (selfTy instanceof TyTraitObject object) traits = object.getTraits();
        else if (selfTy instanceof TyAnon anon) traits = anon.getTraits();
        else return null;
        for (BoundElement<RsTraitItem> be : traitBoundsTransitively(traits)) {
            if (be.getTypedElement() == trait) return be;
        }
        return null;
    }

    private void assembleCandidatesFromObjectTy(@Nonnull TraitRef ref, @Nonnull List<SelectionCandidate> candidates) {
        if (ref.getSelfTy() instanceof TyTraitObject && findObjectBound(ref.getSelfTy(), ref.getTrait().getTypedElement()) != null) {
            candidates.add(SelectionCandidate.ObjectCandidate.INSTANCE);
        }
    }

    private void assembleCandidatesFromProjectedTys(@Nonnull TraitRef ref, @Nonnull List<SelectionCandidate> candidates) {
        Ty selfTy = ref.getSelfTy();
        if (selfTy instanceof TyProjection projection) {
            Substitution subst = projectionSubst(projection);
            Set<BoundElement<RsTraitItem>> seen = new LinkedHashSet<>();
            for (TraitRef bound : RsGenericDeclarationUtil.getBounds(projection.getTrait().getTypedElement())) {
                if (!getCtx().probe(() -> getCtx().combineTypes(FoldUtil.substitute(bound.getSelfTy(), subst), projection).isOk())) continue;
                seen.addAll(RsTraitItemUtil.getFlattenHierarchy(bound.getTrait()));
            }
            for (BoundElement<RsTraitItem> be : seen) {
                if (getCtx().probe(() -> getCtx().combineBoundElements(FoldUtil.substitute(be, subst), ref.getTrait()))) {
                    candidates.add(new SelectionCandidate.ProjectionCandidate(be));
                }
            }
        }
        if (selfTy instanceof TyAnon && findObjectBound(selfTy, ref.getTrait().getTypedElement()) != null) {
            candidates.add(SelectionCandidate.ObjectCandidate.INSTANCE);
        }
    }

    /** {@code Trait<..>::Target<..>} with {@code Self} bound to the projected type. */
    @Nonnull
    private static Substitution projectionSubst(@Nonnull TyProjection projection) {
        Map<TyTypeParameter, Ty> selfMap = new HashMap<>();
        selfMap.put(TyTypeParameter.self(), projection.getType());
        return projection.getTrait().getSubst()
            .plus(projection.getTarget().getSubst())
            .plus(SubstitutionUtil.toTypeSubst(selfMap));
    }

    /** Mirrors rustc's {@code assemble_candidates_for_unsizing}. */
    private void assembleCandidatesForUnsizing(@Nonnull TraitRef ref, @Nonnull CandidateSet candidates) {
        Ty source = ref.getSelfTy();
        TyTypeParameter param = getTypeParamSingle(ref.getTrait().getTypedElement());
        Ty target = param != null ? ref.getTrait().getSubst().get(param) : null;
        if (target == null) return;
        if (target instanceof TyTraitObject) {
            // `T` -> `Trait` and `Trait+Kx+'a` -> `Trait+Ky+'b` (upcasts)
            candidates.list.add(SelectionCandidate.BuiltinUnsizeCandidate.INSTANCE);
        }
        else if (source instanceof TyInfer.TyVar || target instanceof TyInfer.TyVar) {
            candidates.ambiguous = true;
        }
        else if (source instanceof TyArray && target instanceof TySlice
            || source instanceof TyAdt s && target instanceof TyAdt t && s.getItem() == t.getItem()
                && s.getItem() instanceof RsStructItem struct && RsStructItemUtil.getKind(struct) == RsStructKind.STRUCT
            || source instanceof TyTuple st && target instanceof TyTuple tt && st.getTypes().size() == tt.getTypes().size()) {
            candidates.list.add(SelectionCandidate.BuiltinUnsizeCandidate.INSTANCE);
        }
    }

    /**
     * {@code Copy} and {@code Clone} of the scalar types. {@code core} implements them with macros
     * ({@code marker_impls!}, {@code impl_clone!}); this covers a {@code core} whose macros did not expand. Only asked
     * when no impl applies, so an expanded {@code core} never makes the selection ambiguous.
     */
    private void assembleCopyCloneCandidates(@Nonnull TraitRef ref, @Nonnull List<SelectionCandidate> candidates) {
        RsTraitItem trait = ref.getTrait().getTypedElement();
        if (trait != items.getCopy() && trait != items.getClone()) return;
        Ty ty = ref.getSelfTy();
        boolean scalar = (ty instanceof TyPrimitive && !(ty instanceof TyStr))
            || ty instanceof TyInfer.IntVar
            || ty instanceof TyInfer.FloatVar
            || ty instanceof TyNever
            || ty instanceof TyPointer
            || ty instanceof TyFunctionDef
            || ty instanceof TyFunctionPointer
            || (ty instanceof TyReference && !((TyReference) ty).getMutability().isMut());
        if (scalar) {
            candidates.add(new SelectionCandidate.BuiltinCandidate(false));
        }
    }

    private void assembleCandidatesFromImpls(@Nonnull TraitRef ref, @Nonnull List<SelectionCandidate> candidates) {
        assembleDerivedCandidates(ref, candidates);
        processTyFingerprintsWithAliases(ref.getSelfTy(), tyFingerprint -> {
            for (RsCachedImplItem cachedImpl : findPotentialImpls(tyFingerprint)) {
                SelectionCandidate candidate = trySelectCandidate(cachedImpl, ref);
                if (candidate != null) candidates.add(candidate);
            }
            return false;
        });
    }

    /** Only the std traits are taken from `#[derive]`, because they are sure to be resolved correctly. */
    private void assembleDerivedCandidates(@Nonnull TraitRef ref, @Nonnull List<SelectionCandidate> candidates) {
        if (!(ref.getSelfTy() instanceof TyAdt adt) || !(adt.getItem() instanceof RsStructOrEnumItemElement item)) return;
        RsTraitItem trait = ref.getTrait().getTypedElement();
        for (RsTraitItem derived : RsStructOrEnumItemElementUtil.getDerivedTraits(item)) {
            if (derived != trait || !RsTraitItemUtil.isKnownDerivable(derived)) continue;
            if (derived == items.getPartialEq() || derived == items.getPartialOrd()) {
                TraitRef actual = new TraitRef(ref.getSelfTy(), new BoundElement<>(derived).withSubst(ref.getSelfTy()));
                if (!getCtx().probe(() -> getCtx().combineTraitRefs(ref, actual))) continue;
            }
            candidates.add(new SelectionCandidate.ImplCandidate.DerivedTrait(derived));
        }
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

    /** Mirrors rustc's {@code confirm_candidate}. */
    @Nonnull
    private SelectionResult<Selection> confirmCandidate(
        @Nonnull TraitRef ref,
        @Nonnull SelectionCandidate candidate,
        int recursionDepth
    ) {
        if (candidate instanceof SelectionCandidate.BuiltinCandidate builtin) {
            return SelectionResult.ok(confirmBuiltinCandidate(ref, recursionDepth, builtin.hasNested()));
        }
        if (candidate instanceof SelectionCandidate.ParamCandidate param) {
            return SelectionResult.ok(confirmParamCandidate(ref, param));
        }
        if (candidate instanceof SelectionCandidate.ImplCandidate impl) {
            return SelectionResult.ok(confirmImplCandidate(ref, impl, recursionDepth));
        }
        if (candidate instanceof SelectionCandidate.ProjectionCandidate projection) {
            return SelectionResult.ok(confirmProjectionCandidate(ref, projection));
        }
        if (candidate == SelectionCandidate.ObjectCandidate.INSTANCE) {
            return confirmObjectCandidate(ref);
        }
        if (candidate == SelectionCandidate.BuiltinUnsizeCandidate.INSTANCE) {
            return confirmBuiltinUnsizeCandidate(ref, recursionDepth);
        }
        if (candidate == SelectionCandidate.FnPointerCandidate.INSTANCE) {
            return confirmFnPointerCandidate(ref);
        }
        return SelectionResult.ok(new Selection(ref.getTrait().getTypedElement(), Collections.emptyList()));
    }

    @Nonnull
    private Selection confirmBuiltinCandidate(@Nonnull TraitRef ref, int recursionDepth, boolean hasNested) {
        RsTraitItem trait = ref.getTrait().getTypedElement();
        if (!hasNested) return new Selection(trait, Collections.emptyList());
        BuiltinConditions conditions = trait == items.getSized()
            ? sizedConditions(ref.getSelfTy())
            : copyCloneConditions(ref.getSelfTy());
        List<Obligation> obligations = new ArrayList<>();
        if (conditions.nested != null) {
            for (Ty ty : conditions.nested) {
                TyWithObligations<Ty> normalized = getCtx().normalizeAssociatedTypesIn(ty, recursionDepth + 1);
                obligations.addAll(normalized.getObligations());
                obligations.add(new Obligation(recursionDepth + 1,
                    new Predicate.Trait(new TraitRef(normalized.getValue(), new BoundElement<>(trait).withSubst()))));
            }
        }
        return new Selection(trait, obligations);
    }

    @Nonnull
    private Selection confirmProjectionCandidate(@Nonnull TraitRef ref, @Nonnull SelectionCandidate.ProjectionCandidate candidate) {
        TyProjection projection = (TyProjection) ref.getSelfTy();
        getCtx().combineTraitRefs(ref, new TraitRef(projection, FoldUtil.substitute(candidate.getBound(), projectionSubst(projection))));
        return new Selection(ref.getTrait().getTypedElement(), Collections.emptyList());
    }

    @Nonnull
    private SelectionResult<Selection> confirmObjectCandidate(@Nonnull TraitRef ref) {
        BoundElement<RsTraitItem> be = findObjectBound(ref.getSelfTy(), ref.getTrait().getTypedElement());
        if (be == null) return SelectionResult.err();
        getCtx().combineBoundElements(be, ref.getTrait());
        return SelectionResult.ok(new Selection(be.getTypedElement(), Collections.emptyList()));
    }

    @Nonnull
    private SelectionResult<Selection> confirmFnPointerCandidate(@Nonnull TraitRef ref) {
        for (BoundElement<RsTraitItem> impl : getTyFunctionImpls(ref.getSelfTy())) {
            if (impl.getTypedElement() == ref.getTrait().getTypedElement() && combinesWithTraitSubst(impl, ref)) {
                getCtx().combineBoundElements(impl, ref.getTrait());
                Map<TyTypeParameter, Ty> selfMap = new HashMap<>();
                selfMap.put(TyTypeParameter.self(), ref.getSelfTy());
                return SelectionResult.ok(new Selection(impl.getTypedElement(), Collections.emptyList(), SubstitutionUtil.toTypeSubst(selfMap)));
            }
        }
        return SelectionResult.err();
    }

    /** Mirrors rustc's {@code confirm_builtin_unsize_candidate}. */
    @Nonnull
    private SelectionResult<Selection> confirmBuiltinUnsizeCandidate(@Nonnull TraitRef ref, int recursionDepth) {
        RsTraitItem unsizeTrait = ref.getTrait().getTypedElement();
        TyTypeParameter unsizeTypeParam = getTypeParamSingle(unsizeTrait);
        if (unsizeTypeParam == null) return SelectionResult.err();
        Ty rawTarget = ref.getTrait().getSubst().get(unsizeTypeParam);
        if (rawTarget == null) return SelectionResult.err();
        Ty source = getCtx().shallowResolve(ref.getSelfTy());
        Ty target = getCtx().shallowResolve(rawTarget);
        Map<TyTypeParameter, Ty> targetMap = new HashMap<>();
        targetMap.put(unsizeTypeParam, target);
        Substitution okSubst = SubstitutionUtil.toTypeSubst(targetMap);

        if (target instanceof TyTraitObject) {
            // Unsizing to a trait object is always allowed for now
            TypeInferenceMarks.UnsizeToTraitObject.hit();
            return SelectionResult.ok(new Selection(unsizeTrait, Collections.emptyList(), okSubst));
        }
        if (source instanceof TyArray array && target instanceof TySlice slice) {
            if (getCtx().combineTypes(slice.getElementType(), array.getBase()).isOk()) {
                TypeInferenceMarks.UnsizeArrayToSlice.hit();
                return SelectionResult.ok(new Selection(unsizeTrait, Collections.emptyList(), okSubst));
            }
            return SelectionResult.err();
        }
        if (source instanceof TyAdt sourceAdt && target instanceof TyAdt targetAdt && sourceAdt.getItem() instanceof RsStructItem struct) {
            List<RsFieldDecl> fields = RsFieldsOwnerUtil.getFields(struct);
            if (fields.isEmpty()) return SelectionResult.err();
            RsFieldDecl lastField = fields.get(fields.size() - 1);
            RsTypeReference lastFieldTypeRef = lastField.getTypeReference();
            if (lastFieldTypeRef == null) return SelectionResult.err();
            Ty lastFieldType = ExtensionsUtil.getRawType(lastFieldTypeRef);
            Set<TyTypeParameter> unsizingParams = new HashSet<>(typeParametersIn(lastFieldType));
            for (RsFieldDecl field : fields) {
                if (field == lastField) break;
                RsTypeReference fieldTypeRef = field.getTypeReference();
                if (fieldTypeRef == null) continue;
                unsizingParams.removeAll(typeParametersIn(ExtensionsUtil.getRawType(fieldTypeRef)));
            }
            if (unsizingParams.isEmpty()) return SelectionResult.err();

            Substitution sourceSubst = sourceAdt.getTypeParameterValues();
            Substitution targetSubst = targetAdt.getTypeParameterValues();
            // The source struct with the target's unsizing parameters must be equal to the target
            Substitution subst = sourceSubst.mapTypeValues(entry -> {
                if (!unsizingParams.contains(entry.getKey())) return entry.getValue();
                Ty value = targetSubst.get(entry.getKey());
                return value != null ? value : TyUnknown.INSTANCE;
            });
            Ty newStruct = FoldUtil.substitute(struct.getDeclaredType(), subst);
            if (!getCtx().combineTypes(target, newStruct).isOk()) return SelectionResult.err();

            // The nested `TailField<T>: Unsize<TailField<U>>` predicate
            Ty sourceTail = FoldUtil.substitute(lastFieldType, sourceSubst);
            Ty targetTail = FoldUtil.substitute(lastFieldType, targetSubst);
            Obligation nested = new Obligation(recursionDepth + 1,
                new Predicate.Trait(new TraitRef(sourceTail, new BoundElement<>(unsizeTrait).withSubst(targetTail))));
            TypeInferenceMarks.UnsizeStruct.hit();
            return SelectionResult.ok(new Selection(unsizeTrait, List.of(nested), okSubst));
        }
        if (source instanceof TyTuple sourceTuple && target instanceof TyTuple targetTuple) {
            List<Ty> sourceTypes = sourceTuple.getTypes();
            List<Ty> targetTypes = targetTuple.getTypes();
            if (sourceTypes.isEmpty() || targetTypes.isEmpty()) return SelectionResult.err();
            // The source tuple with the target's last element must be equal to the target
            List<Ty> newTypes = new ArrayList<>(sourceTypes.subList(0, sourceTypes.size() - 1));
            newTypes.add(targetTypes.get(targetTypes.size() - 1));
            if (!getCtx().combineTypes(target, new TyTuple(newTypes)).isOk()) return SelectionResult.err();
            Obligation nested = new Obligation(recursionDepth + 1,
                new Predicate.Trait(new TraitRef(sourceTypes.get(sourceTypes.size() - 1),
                    new BoundElement<>(unsizeTrait).withSubst(targetTypes.get(targetTypes.size() - 1)))));
            TypeInferenceMarks.UnsizeTuple.hit();
            return SelectionResult.ok(new Selection(unsizeTrait, List.of(nested), okSubst));
        }
        return SelectionResult.err();
    }

    @Nonnull
    private static Set<TyTypeParameter> typeParametersIn(@Nonnull Ty ty) {
        Set<TyTypeParameter> result = new HashSet<>();
        ty.visitWith(new TypeVisitor() {
            @Override
            public boolean visitTy(@Nonnull Ty t) {
                if (t instanceof TyTypeParameter parameter) {
                    result.add(parameter);
                    return false;
                }
                return t.superVisitWith(this);
            }
        });
        return result;
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
                org.rust.lang.core.psi.ext.impl.RsGenericDeclarationUtil.getPredicates(explicit.getImpl()),
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
        RsTraitItem trait = ref.getTrait().getTypedElement();
        if (trait == items.getPartialEq() || trait == items.getPartialOrd()) {
            getCtx().combineTraitRefs(ref, new TraitRef(ref.getSelfTy(), new BoundElement<>(trait).withSubst(selfTy)));
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
        TyProjection projection = TyProjection.valueOf(lhsType, new BoundElement<>(trait).withSubst(rhsType), new BoundElement<>(assocType).withSubst());
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
        if (selfTy instanceof TyTraitObject object) {
            return lookupAssocTypeInBounds(traitBoundsTransitively(object.getTraits()), res.getImpl(), assocType);
        }
        Ty fromSelection = lookupAssocTypeInSelection(res, assocType);
        if (fromSelection != null) return fromSelection;
        Ty fromFunction = lookupAssocTypeInBounds(getTyFunctionImpls(selfTy), res.getImpl(), assocType);
        if (fromFunction != null) return fromFunction;
        if (selfTy instanceof TyAnon anon) {
            return lookupAssocTypeInBounds(traitBoundsTransitively(anon.getTraits()), res.getImpl(), assocType);
        }
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
        return FoldUtil.substitute(ExtensionsUtil.getRawType(typeRef), selection.getSubst().plus(substFromTraitToImpl(assocDef, assocImpl)));
    }

    /** The generic arguments of a trait's associated type, keyed by the parameters of the impl's associated type. */
    @Nonnull
    private static Substitution substFromTraitToImpl(@Nonnull BoundElement<RsTypeAlias> boundAssocDef, @Nonnull RsTypeAlias assocImpl) {
        RsTypeAlias assocDef = boundAssocDef.getTypedElement();
        Map<TyTypeParameter, TyTypeParameter> typeKeys = new HashMap<>();
        List<RsTypeParameter> defTypeParameters = assocDef.getTypeParameters();
        List<RsTypeParameter> implTypeParameters = assocImpl.getTypeParameters();
        for (int i = 0; i < Math.min(defTypeParameters.size(), implTypeParameters.size()); i++) {
            typeKeys.put(TyTypeParameter.named(defTypeParameters.get(i)), TyTypeParameter.named(implTypeParameters.get(i)));
        }
        Map<CtConstParameter, CtConstParameter> constKeys = new HashMap<>();
        List<RsConstParameter> defConstParameters = assocDef.getConstParameters();
        List<RsConstParameter> implConstParameters = assocImpl.getConstParameters();
        for (int i = 0; i < Math.min(defConstParameters.size(), implConstParameters.size()); i++) {
            constKeys.put(new CtConstParameter(defConstParameters.get(i)), new CtConstParameter(implConstParameters.get(i)));
        }
        Substitution subst = boundAssocDef.getSubst();
        Map<TyTypeParameter, Ty> typeSubst = new HashMap<>();
        for (Map.Entry<TyTypeParameter, Ty> entry : subst.getTypeSubst().entrySet()) {
            typeSubst.put(typeKeys.getOrDefault(entry.getKey(), entry.getKey()), entry.getValue());
        }
        Map<CtConstParameter, Const> constSubst = new HashMap<>();
        for (Map.Entry<CtConstParameter, Const> entry : subst.getConstSubst().entrySet()) {
            constSubst.put(constKeys.getOrDefault(entry.getKey(), entry.getKey()), entry.getValue());
        }
        return new Substitution(typeSubst, subst.getRegionSubst(), constSubst);
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
