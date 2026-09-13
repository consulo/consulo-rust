/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.lang.core.types.infer;

import jakarta.annotation.Nonnull;
import org.rust.lang.core.resolve.ImplLookup;
import org.rust.lang.core.resolve.SelectionResult;
import org.rust.lang.core.types.TraitRef;
import org.rust.lang.core.types.ty.Ty;
import org.rust.lang.core.types.ty.TyInfer;

import java.util.*;
import org.rust.stdext.RsResult;

public class FulfillmentContext {
    @Nonnull
    private final RsInferenceContext myCtx;
    @Nonnull
    private final ImplLookup myLookup;
    @Nonnull
    private final ObligationForest myObligations;

    public FulfillmentContext(@Nonnull RsInferenceContext ctx, @Nonnull ImplLookup lookup) {
        this(ctx, lookup, false);
    }

    public FulfillmentContext(@Nonnull RsInferenceContext ctx, @Nonnull ImplLookup lookup, boolean traceObligations) {
        myCtx = ctx;
        myLookup = lookup;
        myObligations = new ObligationForest(traceObligations);
    }

    @Nonnull
    public RsInferenceContext getCtx() {
        return myCtx;
    }

    @Nonnull
    public ImplLookup getLookup() {
        return myLookup;
    }

    @Nonnull
    public Iterable<PendingPredicateObligation> getPendingObligations() {
        return myObligations.getPendingObligations();
    }

    @Nonnull
    public List<ObligationForest.Node> getRootNodes() {
        return myObligations.getRoots();
    }

    @Nonnull
    public Map<ObligationForest.Node, List<ObligationForest.Node>> getParentToChildren() {
        return myObligations.getParentToChildren();
    }

    public void registerPredicateObligation(@Nonnull Obligation obligation) {
        Obligation resolved = myCtx.resolveTypeVarsIfPossible(obligation);
        myObligations.registerObligationAt(new PendingPredicateObligation(resolved), null);
    }

    public void registerPredicateObligations(@Nonnull List<Obligation> obligations) {
        for (Obligation obligation : obligations) {
            registerPredicateObligation(obligation);
        }
    }

    public void selectWherePossible() {
        ObligationForest.ProcessObligationsResult result;
        do {
            result = myObligations.processObligations(this::processPredicate);
        } while (!result.isStalled());
    }

    public boolean selectUntilError() {
        ObligationForest.ProcessObligationsResult result;
        do {
            result = myObligations.processObligations(this::processPredicate, true);
            if (result.isHasErrors()) return false;
        } while (!result.isStalled());
        return true;
    }

    @Nonnull
    private ProcessPredicateResult processPredicate(@Nonnull PendingPredicateObligation pendingObligation) {
        Obligation obligation = pendingObligation.getObligation();
        List<Ty> stalledOn = pendingObligation.getStalledOn();
        if (!stalledOn.isEmpty()) {
            boolean nothingChanged = true;
            for (Ty ty : stalledOn) {
                Ty resolved = myCtx.shallowResolve(ty);
                if (!resolved.equals(ty)) {
                    nothingChanged = false;
                    break;
                }
            }
            if (nothingChanged) return ProcessPredicateResult.NoChanges;
            pendingObligation.setStalledOn(Collections.emptyList());
        }

        obligation.setPredicate(myCtx.resolveTypeVarsIfPossible(obligation.getPredicate()));
        Predicate predicate = obligation.getPredicate();

        if (predicate instanceof Predicate.Trait) {
            Predicate.Trait traitPred = (Predicate.Trait) predicate;
            if (traitPred.getTrait().getSelfTy() instanceof TyInfer.TyVar) {
                return ProcessPredicateResult.NoChanges;
            }

            SelectionResult<?> impl = myLookup.select(traitPred.getTrait(), traitPred.getConstness(), obligation.getRecursionDepth());
            if (impl instanceof SelectionResult.Err) {
                return ProcessPredicateResult.Err;
            } else if (impl instanceof SelectionResult.Ambiguous) {
                pendingObligation.setStalledOn(traitRefTypeVars(traitPred.getTrait()));
                return ProcessPredicateResult.NoChanges;
            } else if (impl instanceof SelectionResult.Ok) {
                Object result = ((SelectionResult.Ok<?>) impl).getResult();
                // The result has nestedObligations - extract and wrap
                List<Obligation> nested = getNestedObligations(result);
                List<PendingPredicateObligation> children = new ArrayList<>();
                for (Obligation o : nested) {
                    children.add(new PendingPredicateObligation(o));
                }
                return new ProcessPredicateResult.Ok(children);
            }
        } else if (predicate instanceof Predicate.Equate) {
            Predicate.Equate equate = (Predicate.Equate) predicate;
            myCtx.combineTypes(equate.getTy1(), equate.getTy2());
            return new ProcessPredicateResult.Ok(Collections.emptyList());
        } else if (predicate instanceof Predicate.Projection) {
            Predicate.Projection proj = (Predicate.Projection) predicate;
            if (proj.getProjectionTy().getType() instanceof TyInfer.TyVar) {
                return ProcessPredicateResult.NoChanges;
            }
            TyWithObligations<Ty> result = myCtx.optNormalizeProjectionType(proj.getProjectionTy(), obligation.getRecursionDepth());
            if (result == null) {
                pendingObligation.setStalledOn(traitRefTypeVars(proj.getProjectionTy().getTraitRef()));
                return ProcessPredicateResult.NoChanges;
            }
            Object combineResult = myCtx.combineTypes(proj.getTy(), result.getValue());
            if (isOk(combineResult)) {
                List<PendingPredicateObligation> children = new ArrayList<>();
                for (Obligation o : result.getObligations()) {
                    children.add(new PendingPredicateObligation(o));
                }
                return new ProcessPredicateResult.Ok(children);
            }
            return ProcessPredicateResult.Err;
        }

        return ProcessPredicateResult.NoChanges;
    }

    @SuppressWarnings("unchecked")
    @Nonnull
    private List<Obligation> getNestedObligations(@Nonnull Object result) {
        // Use reflection or duck typing to get nested obligations from selection result
        try {
            java.lang.reflect.Method m = result.getClass().getMethod("getNestedObligations");
            return (List<Obligation>) m.invoke(result);
        } catch (Exception e) {
            return Collections.emptyList();
        }
    }

    private boolean isOk(@Nonnull Object result) {
        try {
            return result instanceof org.rust.stdext.RsResult.Ok;
        } catch (Exception e) {
            return true;
        }
    }

    @Nonnull
    private List<Ty> traitRefTypeVars(@Nonnull TraitRef ref) {
        TraitRef resolved = myCtx.resolveTypeVarsIfPossible(ref);
        return new ArrayList<>(FoldUtil.collectInferTys(resolved));
    }

    @Nonnull
    public <T> T register(@Nonnull TyWithObligations<T> tyo) {
        for (Obligation o : tyo.getObligations()) {
            registerPredicateObligation(o);
        }
        return tyo.getValue();
    }
}
