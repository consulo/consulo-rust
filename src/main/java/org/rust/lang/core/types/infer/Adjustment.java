/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.lang.core.types.infer;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.rust.lang.core.types.ty.*;

/**
 * Represents type adjustments applied to expressions during type inference.
 */
public abstract class Adjustment implements TypeFoldable<Adjustment> {
    @NotNull
    public abstract Ty getTarget();

    @Override
    public boolean superVisitWith(@NotNull TypeVisitor visitor) {
        return getTarget().visitWith(visitor);
    }

    public static class NeverToAny extends Adjustment {
        @NotNull
        private final Ty myTarget;

        public NeverToAny(@NotNull Ty target) {
            myTarget = target;
        }

        @NotNull
        @Override
        public Ty getTarget() {
            return myTarget;
        }

        @NotNull
        @Override
        public Adjustment superFoldWith(@NotNull TypeFolder folder) {
            return new NeverToAny(myTarget.foldWith(folder));
        }
    }

    public static class Deref extends Adjustment {
        @NotNull
        private final Ty myTarget;
        /** Non-null if dereference has been done using Deref/DerefMut trait */
        @Nullable
        private final Mutability myOverloaded;

        public Deref(@NotNull Ty target, @Nullable Mutability overloaded) {
            myTarget = target;
            myOverloaded = overloaded;
        }

        @NotNull
        @Override
        public Ty getTarget() {
            return myTarget;
        }

        @Nullable
        public Mutability getOverloaded() {
            return myOverloaded;
        }

        @NotNull
        @Override
        public Adjustment superFoldWith(@NotNull TypeFolder folder) {
            return new Deref(myTarget.foldWith(folder), myOverloaded);
        }
    }

    public static class BorrowReference extends Adjustment {
        @NotNull
        private final TyReference myTarget;

        public BorrowReference(@NotNull TyReference target) {
            myTarget = target;
        }

        @NotNull
        @Override
        public TyReference getTarget() {
            return myTarget;
        }

        @NotNull
        public AutoBorrowMutability getMutability() {
            if (myTarget.getMutability() == Mutability.MUTABLE) {
                return new AutoBorrowMutability.Mutable(false);
            }
            return AutoBorrowMutability.Immutable;
        }

        @NotNull
        @Override
        public Adjustment superFoldWith(@NotNull TypeFolder folder) {
            return new BorrowReference((TyReference) myTarget.foldWith(folder));
        }
    }

    public static class BorrowPointer extends Adjustment {
        @NotNull
        private final TyPointer myTarget;

        public BorrowPointer(@NotNull TyPointer target) {
            myTarget = target;
        }

        @NotNull
        @Override
        public TyPointer getTarget() {
            return myTarget;
        }

        @NotNull
        public Mutability getMutability() {
            return myTarget.getMutability();
        }

        @NotNull
        @Override
        public Adjustment superFoldWith(@NotNull TypeFolder folder) {
            return new BorrowPointer((TyPointer) myTarget.foldWith(folder));
        }
    }

    public static class ClosureFnPointer extends Adjustment {
        @NotNull
        private final TyFunctionPointer myTarget;

        public ClosureFnPointer(@NotNull TyFunctionPointer target) {
            myTarget = target;
        }

        @NotNull
        @Override
        public TyFunctionPointer getTarget() {
            return myTarget;
        }

        @NotNull
        @Override
        public Adjustment superFoldWith(@NotNull TypeFolder folder) {
            return new ClosureFnPointer((TyFunctionPointer) myTarget.foldWith(folder));
        }
    }

    public static class ReifyFnPointer extends Adjustment {
        @NotNull
        private final TyFunctionPointer myTarget;

        public ReifyFnPointer(@NotNull TyFunctionPointer target) {
            myTarget = target;
        }

        @NotNull
        @Override
        public TyFunctionPointer getTarget() {
            return myTarget;
        }

        @NotNull
        @Override
        public Adjustment superFoldWith(@NotNull TypeFolder folder) {
            return new ReifyFnPointer((TyFunctionPointer) myTarget.foldWith(folder));
        }
    }

    public static class UnsafeFnPointer extends Adjustment {
        @NotNull
        private final TyFunctionPointer myTarget;

        public UnsafeFnPointer(@NotNull TyFunctionPointer target) {
            myTarget = target;
        }

        @NotNull
        @Override
        public TyFunctionPointer getTarget() {
            return myTarget;
        }

        @NotNull
        @Override
        public Adjustment superFoldWith(@NotNull TypeFolder folder) {
            return new UnsafeFnPointer((TyFunctionPointer) myTarget.foldWith(folder));
        }
    }

    public static class MutToConstPointer extends Adjustment {
        @NotNull
        private final TyPointer myTarget;

        public MutToConstPointer(@NotNull TyPointer target) {
            myTarget = target;
        }

        @NotNull
        @Override
        public TyPointer getTarget() {
            return myTarget;
        }

        @NotNull
        @Override
        public Adjustment superFoldWith(@NotNull TypeFolder folder) {
            return new MutToConstPointer((TyPointer) myTarget.foldWith(folder));
        }
    }

    public static class Unsize extends Adjustment {
        @NotNull
        private final Ty myTarget;

        public Unsize(@NotNull Ty target) {
            myTarget = target;
        }

        @NotNull
        @Override
        public Ty getTarget() {
            return myTarget;
        }

        @NotNull
        @Override
        public Adjustment superFoldWith(@NotNull TypeFolder folder) {
            return new Unsize(myTarget.foldWith(folder));
        }
    }
}
