/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.lang.core.types.infer;

import org.jetbrains.annotations.NotNull;
import org.rust.lang.core.types.TraitRef;
import org.rust.lang.core.types.ty.Ty;
import org.rust.lang.core.types.ty.TyProjection;

import java.util.Objects;

public abstract class Predicate implements TypeFoldable<Predicate> {
    private Predicate() {}

    /** where T : Bar<A,B,C> */
    public static final class Trait extends Predicate {
        @NotNull
        private final TraitRef myTrait;
        @NotNull
        private final BoundConstness myConstness;

        public Trait(@NotNull TraitRef trait) {
            this(trait, BoundConstness.NotConst);
        }

        public Trait(@NotNull TraitRef trait, @NotNull BoundConstness constness) {
            myTrait = trait;
            myConstness = constness;
        }

        @NotNull
        public TraitRef getTrait() {
            return myTrait;
        }

        @NotNull
        public BoundConstness getConstness() {
            return myConstness;
        }

        @Override
        @NotNull
        public Trait superFoldWith(@NotNull TypeFolder folder) {
            return new Trait(myTrait.foldWith(folder), myConstness);
        }

        @Override
        public boolean superVisitWith(@NotNull TypeVisitor visitor) {
            return myTrait.visitWith(visitor);
        }

        @Override
        public String toString() {
            return myTrait.toString();
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            Trait trait = (Trait) o;
            return Objects.equals(myTrait, trait.myTrait) && myConstness == trait.myConstness;
        }

        @Override
        public int hashCode() {
            return Objects.hash(myTrait, myConstness);
        }
    }

    /** where <T as TraitRef>::Name == X */
    public static final class Projection extends Predicate {
        @NotNull
        private final TyProjection myProjectionTy;
        @NotNull
        private final Ty myTy;

        public Projection(@NotNull TyProjection projectionTy, @NotNull Ty ty) {
            myProjectionTy = projectionTy;
            myTy = ty;
        }

        @NotNull
        public TyProjection getProjectionTy() {
            return myProjectionTy;
        }

        @NotNull
        public Ty getTy() {
            return myTy;
        }

        @Override
        @NotNull
        public Projection superFoldWith(@NotNull TypeFolder folder) {
            return new Projection((TyProjection) myProjectionTy.foldWith(folder), myTy.foldWith(folder));
        }

        @Override
        public boolean superVisitWith(@NotNull TypeVisitor visitor) {
            return myProjectionTy.visitWith(visitor) || myTy.visitWith(visitor);
        }

        @Override
        public String toString() {
            return myProjectionTy + " == " + myTy;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            Projection that = (Projection) o;
            return Objects.equals(myProjectionTy, that.myProjectionTy) && Objects.equals(myTy, that.myTy);
        }

        @Override
        public int hashCode() {
            return Objects.hash(myProjectionTy, myTy);
        }
    }

    /** where T1 == T2 */
    public static final class Equate extends Predicate {
        @NotNull
        private final Ty myTy1;
        @NotNull
        private final Ty myTy2;

        public Equate(@NotNull Ty ty1, @NotNull Ty ty2) {
            myTy1 = ty1;
            myTy2 = ty2;
        }

        @NotNull
        public Ty getTy1() {
            return myTy1;
        }

        @NotNull
        public Ty getTy2() {
            return myTy2;
        }

        @Override
        @NotNull
        public Predicate superFoldWith(@NotNull TypeFolder folder) {
            return new Equate(myTy1.foldWith(folder), myTy2.foldWith(folder));
        }

        @Override
        public boolean superVisitWith(@NotNull TypeVisitor visitor) {
            return myTy1.visitWith(visitor) || myTy2.visitWith(visitor);
        }

        @Override
        public String toString() {
            return myTy1 + " == " + myTy2;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            Equate equate = (Equate) o;
            return Objects.equals(myTy1, equate.myTy1) && Objects.equals(myTy2, equate.myTy2);
        }

        @Override
        public int hashCode() {
            return Objects.hash(myTy1, myTy2);
        }
    }
}
