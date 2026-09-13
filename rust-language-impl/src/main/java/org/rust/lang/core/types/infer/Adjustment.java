/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.lang.core.types.infer;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import org.rust.lang.core.types.ty.*;

/**
 * Represents type adjustments applied to expressions during type inference.
 */
public abstract class Adjustment implements TypeFoldable<Adjustment> {
    @Nonnull
    public abstract Ty getTarget();

    @Override
    public boolean superVisitWith(@Nonnull TypeVisitor visitor) {
        return getTarget().visitWith(visitor);
    }

    public static class NeverToAny extends Adjustment {
        @Nonnull
        private final Ty myTarget;

        public NeverToAny(@Nonnull Ty target) {
            myTarget = target;
        }

        @Nonnull
        @Override
        public Ty getTarget() {
            return myTarget;
        }

        @Nonnull
        @Override
        public Adjustment superFoldWith(@Nonnull TypeFolder folder) {
            return new NeverToAny(myTarget.foldWith(folder));
        }
    }

    public static class Deref extends Adjustment {
        @Nonnull
        private final Ty myTarget;
        /** Non-null if dereference has been done using Deref/DerefMut trait */
        @Nullable
        private final Mutability myOverloaded;

        public Deref(@Nonnull Ty target, @Nullable Mutability overloaded) {
            myTarget = target;
            myOverloaded = overloaded;
        }

        @Nonnull
        @Override
        public Ty getTarget() {
            return myTarget;
        }

        @Nullable
        public Mutability getOverloaded() {
            return myOverloaded;
        }

        @Nonnull
        @Override
        public Adjustment superFoldWith(@Nonnull TypeFolder folder) {
            return new Deref(myTarget.foldWith(folder), myOverloaded);
        }
    }

    public static class BorrowReference extends Adjustment {
        @Nonnull
        private final TyReference myTarget;

        public BorrowReference(@Nonnull TyReference target) {
            myTarget = target;
        }

        @Nonnull
        @Override
        public TyReference getTarget() {
            return myTarget;
        }

        @Nonnull
        public AutoBorrowMutability getMutability() {
            if (myTarget.getMutability() == Mutability.MUTABLE) {
                return new AutoBorrowMutability.Mutable(false);
            }
            return AutoBorrowMutability.Immutable;
        }

        @Nonnull
        @Override
        public Adjustment superFoldWith(@Nonnull TypeFolder folder) {
            return new BorrowReference((TyReference) myTarget.foldWith(folder));
        }
    }

    public static class BorrowPointer extends Adjustment {
        @Nonnull
        private final TyPointer myTarget;

        public BorrowPointer(@Nonnull TyPointer target) {
            myTarget = target;
        }

        @Nonnull
        @Override
        public TyPointer getTarget() {
            return myTarget;
        }

        @Nonnull
        public Mutability getMutability() {
            return myTarget.getMutability();
        }

        @Nonnull
        @Override
        public Adjustment superFoldWith(@Nonnull TypeFolder folder) {
            return new BorrowPointer((TyPointer) myTarget.foldWith(folder));
        }
    }

    public static class ClosureFnPointer extends Adjustment {
        @Nonnull
        private final TyFunctionPointer myTarget;

        public ClosureFnPointer(@Nonnull TyFunctionPointer target) {
            myTarget = target;
        }

        @Nonnull
        @Override
        public TyFunctionPointer getTarget() {
            return myTarget;
        }

        @Nonnull
        @Override
        public Adjustment superFoldWith(@Nonnull TypeFolder folder) {
            return new ClosureFnPointer((TyFunctionPointer) myTarget.foldWith(folder));
        }
    }

    public static class ReifyFnPointer extends Adjustment {
        @Nonnull
        private final TyFunctionPointer myTarget;

        public ReifyFnPointer(@Nonnull TyFunctionPointer target) {
            myTarget = target;
        }

        @Nonnull
        @Override
        public TyFunctionPointer getTarget() {
            return myTarget;
        }

        @Nonnull
        @Override
        public Adjustment superFoldWith(@Nonnull TypeFolder folder) {
            return new ReifyFnPointer((TyFunctionPointer) myTarget.foldWith(folder));
        }
    }

    public static class UnsafeFnPointer extends Adjustment {
        @Nonnull
        private final TyFunctionPointer myTarget;

        public UnsafeFnPointer(@Nonnull TyFunctionPointer target) {
            myTarget = target;
        }

        @Nonnull
        @Override
        public TyFunctionPointer getTarget() {
            return myTarget;
        }

        @Nonnull
        @Override
        public Adjustment superFoldWith(@Nonnull TypeFolder folder) {
            return new UnsafeFnPointer((TyFunctionPointer) myTarget.foldWith(folder));
        }
    }

    public static class MutToConstPointer extends Adjustment {
        @Nonnull
        private final TyPointer myTarget;

        public MutToConstPointer(@Nonnull TyPointer target) {
            myTarget = target;
        }

        @Nonnull
        @Override
        public TyPointer getTarget() {
            return myTarget;
        }

        @Nonnull
        @Override
        public Adjustment superFoldWith(@Nonnull TypeFolder folder) {
            return new MutToConstPointer((TyPointer) myTarget.foldWith(folder));
        }
    }

    public static class Unsize extends Adjustment {
        @Nonnull
        private final Ty myTarget;

        public Unsize(@Nonnull Ty target) {
            myTarget = target;
        }

        @Nonnull
        @Override
        public Ty getTarget() {
            return myTarget;
        }

        @Nonnull
        @Override
        public Adjustment superFoldWith(@Nonnull TypeFolder folder) {
            return new Unsize(myTarget.foldWith(folder));
        }
    }
}
