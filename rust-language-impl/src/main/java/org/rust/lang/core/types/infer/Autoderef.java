/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.lang.core.types.infer;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import org.rust.lang.core.resolve.ImplLookup;
import org.rust.lang.core.resolve.NameResolution;
import org.rust.lang.core.resolve.KnownItems;
import org.rust.lang.core.types.ty.*;

import java.util.*;

public class Autoderef implements Iterable<Ty> {
    @Nonnull
    private final ImplLookup myLookup;
    @Nonnull
    private final RsInferenceContext myCtx;
    @Nonnull
    private final Ty myBaseTy;
    private final Set<Ty> myVisitedTys = new HashSet<>();
    private final List<AutoderefStep> mySteps = new ArrayList<>();
    private final List<Obligation> myObligations = new ArrayList<>();

    public Autoderef(@Nonnull ImplLookup lookup, @Nonnull RsInferenceContext ctx, @Nonnull Ty baseTy) {
        myLookup = lookup;
        myCtx = ctx;
        myBaseTy = ctx.resolveTypeVarsIfPossible(baseTy);
    }

    @Nonnull
    @Override
    public Iterator<Ty> iterator() {
        return new Iterator<Ty>() {
            private Ty myNext;
            /** -2: not started, -1: next element not computed yet, 0: exhausted, 1: next element ready */
            private int myState = -2;
            private int myProduced = 0;

            private void calcNext() {
                if (myState == -2) {
                    myNext = myBaseTy;
                }
                else if (myProduced >= NameResolution.DEFAULT_RECURSION_LIMIT) {
                    myNext = null;
                }
                else {
                    myNext = derefOnce(myNext);
                }
                myState = myNext == null ? 0 : 1;
            }

            @Override
            public boolean hasNext() {
                if (myState < 0) calcNext();
                return myState == 1;
            }

            @Override
            public Ty next() {
                if (myState < 0) calcNext();
                if (myState == 0) throw new NoSuchElementException();
                Ty result = myNext;
                myState = -1;
                myProduced++;
                return result;
            }
        };
    }

    /**
     * One dereference step away from {@code from}, recording it in {@link #steps()}, or {@code null} when
     * {@code from} cannot be dereferenced further or has already been visited.
     */
    @Nullable
    private Ty derefOnce(@Nonnull Ty from) {
        if (!myVisitedTys.add(from)) return null;
        TyWithObligations<Ty> deref = myLookup.deref(from);
        Ty to = null;
        if (deref != null) {
            if (!deref.getObligations().isEmpty()) {
                FulfillmentContext fulfillment = new FulfillmentContext(myCtx, myLookup);
                fulfillment.registerPredicateObligations(deref.getObligations());
                fulfillment.selectWherePossible();
                for (PendingPredicateObligation pending : fulfillment.getPendingObligations()) {
                    myObligations.add(pending.getObligation());
                }
            }
            to = myCtx.resolveTypeVarsWithObligations(deref.getValue());
        }
        if (to == null && from instanceof TyArray) {
            to = new TySlice(((TyArray) from).getBase());
        }
        if (to != null) {
            mySteps.add(new AutoderefStep(from, to));
        }
        return to;
    }

    @Nonnull
    public List<AutoderefStep> steps() {
        return new ArrayList<>(mySteps);
    }

    @Nonnull
    public List<Obligation> obligations() {
        return new ArrayList<>(myObligations);
    }

    public int stepCount() {
        return mySteps.size();
    }

    public static class AutoderefStep {
        @Nonnull
        private final Ty myFrom;
        @Nonnull
        private final Ty myTo;

        public AutoderefStep(@Nonnull Ty from, @Nonnull Ty to) {
            myFrom = from;
            myTo = to;
        }

        @Nonnull
        public Ty getFrom() {
            return myFrom;
        }

        @Nonnull
        public Ty getTo() {
            return myTo;
        }

        @Nonnull
        public AutoderefKind getKind(@Nonnull KnownItems items) {
            if (myFrom instanceof TyReference || myFrom instanceof TyPointer) {
                return AutoderefKind.Builtin;
            }
            if (myFrom instanceof TyAdt && ((TyAdt) myFrom).getItem() == items.getBox()) {
                return AutoderefKind.Builtin;
            }
            if (myFrom instanceof TyArray && myTo instanceof TySlice) {
                return AutoderefKind.ArrayToSlice;
            }
            return AutoderefKind.Overloaded;
        }
    }

    public enum AutoderefKind {
        Builtin,
        Overloaded,
        ArrayToSlice
    }

    @Nonnull
    public static List<Adjustment.Deref> toAdjustments(@Nonnull List<AutoderefStep> steps, @Nonnull KnownItems items) {
        List<Adjustment.Deref> result = new ArrayList<>();
        for (AutoderefStep step : steps) {
            AutoderefKind kind = step.getKind(items);
            if (kind == AutoderefKind.Builtin) {
                result.add(new Adjustment.Deref(step.myTo, null));
            } else if (kind == AutoderefKind.Overloaded) {
                result.add(new Adjustment.Deref(step.myTo, Mutability.IMMUTABLE));
            }
            // ArrayToSlice -> null, skipped
        }
        return result;
    }
}
