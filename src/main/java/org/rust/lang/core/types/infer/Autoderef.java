/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.lang.core.types.infer;

import org.jetbrains.annotations.NotNull;
import org.rust.lang.core.resolve.ImplLookup;
import org.rust.lang.core.resolve.KnownItems;
import org.rust.lang.core.types.ty.*;

import java.util.*;

public class Autoderef implements Iterable<Ty> {
    @NotNull
    private final ImplLookup myLookup;
    @NotNull
    private final RsInferenceContext myCtx;
    @NotNull
    private final Ty myBaseTy;
    private final Set<Ty> myVisitedTys = new HashSet<>();
    private final List<AutoderefStep> mySteps = new ArrayList<>();
    private final List<Obligation> myObligations = new ArrayList<>();

    public Autoderef(@NotNull ImplLookup lookup, @NotNull RsInferenceContext ctx, @NotNull Ty baseTy) {
        myLookup = lookup;
        myCtx = ctx;
        myBaseTy = ctx.resolveTypeVarsIfPossible(baseTy);
    }

    @NotNull
    @Override
    public Iterator<Ty> iterator() {
        return new Iterator<Ty>() {
            private Ty myCurrent = myBaseTy;
            private boolean myFirst = true;

            @Override
            public boolean hasNext() {
                return myCurrent != null;
            }

            @Override
            public Ty next() {
                if (myCurrent == null) throw new NoSuchElementException();
                Ty result = myCurrent;
                if (myFirst) {
                    myFirst = false;
                } else {
                    // already advanced
                }
                advance();
                return result;
            }

            private void advance() {
                if (myCurrent == null) return;
                if (!myVisitedTys.add(myCurrent)) {
                    myCurrent = null;
                    return;
                }
                TyWithObligations<Ty> deref = myLookup.deref(myCurrent);
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
                if (to == null && myCurrent instanceof TyArray) {
                    to = new TySlice(((TyArray) myCurrent).getBase());
                }
                if (to != null) {
                    mySteps.add(new AutoderefStep(myCurrent, to));
                }
                myCurrent = to;
            }
        };
    }

    @NotNull
    public List<AutoderefStep> steps() {
        return new ArrayList<>(mySteps);
    }

    @NotNull
    public List<Obligation> obligations() {
        return new ArrayList<>(myObligations);
    }

    public int stepCount() {
        return mySteps.size();
    }

    public static class AutoderefStep {
        @NotNull
        private final Ty myFrom;
        @NotNull
        private final Ty myTo;

        public AutoderefStep(@NotNull Ty from, @NotNull Ty to) {
            myFrom = from;
            myTo = to;
        }

        @NotNull
        public Ty getFrom() {
            return myFrom;
        }

        @NotNull
        public Ty getTo() {
            return myTo;
        }

        @NotNull
        public AutoderefKind getKind(@NotNull KnownItems items) {
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

    @NotNull
    public static List<Adjustment.Deref> toAdjustments(@NotNull List<AutoderefStep> steps, @NotNull KnownItems items) {
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
