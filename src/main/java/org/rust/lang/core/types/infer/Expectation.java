/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.lang.core.types.infer;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.rust.lang.core.types.ty.*;

/**
 * When type-checking an expression, we propagate downward
 * whatever type hint we are able in the form of an Expectation.
 */
public abstract class Expectation {
    private Expectation() {}

    /** We know nothing about what type this expression should have */
    public static final Expectation NoExpectation = new Expectation() {
        @Override
        public String toString() { return "NoExpectation"; }
    };

    /** This expression should have the type given (or some subtype) */
    public static final class ExpectHasType extends Expectation {
        @NotNull
        private final Ty myTy;

        public ExpectHasType(@NotNull Ty ty) {
            myTy = ty;
        }

        @NotNull
        public Ty getTy() {
            return myTy;
        }
    }

    /** This expression will be cast to the Ty */
    public static final class ExpectCastableToType extends Expectation {
        @NotNull
        private final Ty myTy;

        public ExpectCastableToType(@NotNull Ty ty) {
            myTy = ty;
        }

        @NotNull
        public Ty getTy() {
            return myTy;
        }
    }

    /**
     * This rvalue expression will be wrapped in & or Box and coerced
     * to &Ty or Box<Ty>, respectively.
     */
    public static final class ExpectRvalueLikeUnsized extends Expectation {
        @NotNull
        private final Ty myTy;

        public ExpectRvalueLikeUnsized(@NotNull Ty ty) {
            myTy = ty;
        }

        @NotNull
        public Ty getTy() {
            return myTy;
        }
    }

    @NotNull
    private Expectation resolve(@NotNull RsInferenceContext ctx) {
        if (this instanceof ExpectHasType) {
            return new ExpectHasType(ctx.resolveTypeVarsIfPossible(((ExpectHasType) this).myTy));
        } else if (this instanceof ExpectCastableToType) {
            return new ExpectCastableToType(ctx.resolveTypeVarsIfPossible(((ExpectCastableToType) this).myTy));
        } else if (this instanceof ExpectRvalueLikeUnsized) {
            return new ExpectRvalueLikeUnsized(ctx.resolveTypeVarsIfPossible(((ExpectRvalueLikeUnsized) this).myTy));
        }
        return this;
    }

    @Nullable
    public Ty tyAsNullable(@NotNull RsInferenceContext ctx) {
        Expectation resolved = this.resolve(ctx);
        if (resolved instanceof ExpectHasType) return ((ExpectHasType) resolved).myTy;
        if (resolved instanceof ExpectCastableToType) return ((ExpectCastableToType) resolved).myTy;
        if (resolved instanceof ExpectRvalueLikeUnsized) return ((ExpectRvalueLikeUnsized) resolved).myTy;
        return null;
    }

    @Nullable
    public Ty onlyHasTy(@NotNull RsInferenceContext ctx) {
        if (this instanceof ExpectHasType) {
            return ctx.resolveTypeVarsIfPossible(((ExpectHasType) this).myTy);
        }
        return null;
    }

    @NotNull
    public static Expectation rvalueHint(@NotNull Ty ty) {
        Ty tail = TyUtil.structTail(ty);
        if (tail == null) tail = ty;
        if (tail instanceof TyUnknown) return NoExpectation;
        if (tail instanceof TySlice || tail instanceof TyTraitObject || tail instanceof TyStr) {
            return new ExpectRvalueLikeUnsized(ty);
        }
        return new ExpectHasType(ty);
    }

    @NotNull
    public static Expectation maybeHasType(@Nullable Ty ty) {
        if (ty == null || ty instanceof TyUnknown) return NoExpectation;
        return new ExpectHasType(ty);
    }
}
