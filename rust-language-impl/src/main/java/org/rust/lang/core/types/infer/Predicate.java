/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.lang.core.types.infer;

import jakarta.annotation.Nonnull;
import org.rust.lang.core.types.TraitRef;
import org.rust.lang.core.types.ty.Ty;
import org.rust.lang.core.types.ty.TyProjection;

import java.util.Objects;

public abstract class Predicate implements TypeFoldable<Predicate> {
    private Predicate() {}

    /** where T : Bar<A,B,C> */
    public static final class Trait extends Predicate {
        @Nonnull
        private final TraitRef myTrait;
        @Nonnull
        private final BoundConstness myConstness;

        public Trait(@Nonnull TraitRef trait) {
            this(trait, BoundConstness.NotConst);
        }

        public Trait(@Nonnull TraitRef trait, @Nonnull BoundConstness constness) {
            myTrait = trait;
            myConstness = constness;
        }

        @Nonnull
        public TraitRef getTrait() {
            return myTrait;
        }

        @Nonnull
        public BoundConstness getConstness() {
            return myConstness;
        }

        @Override
        @Nonnull
        public Trait superFoldWith(@Nonnull TypeFolder folder) {
            return new Trait(myTrait.foldWith(folder), myConstness);
        }

        @Override
        public boolean superVisitWith(@Nonnull TypeVisitor visitor) {
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
        @Nonnull
        private final TyProjection myProjectionTy;
        @Nonnull
        private final Ty myTy;

        public Projection(@Nonnull TyProjection projectionTy, @Nonnull Ty ty) {
            myProjectionTy = projectionTy;
            myTy = ty;
        }

        @Nonnull
        public TyProjection getProjectionTy() {
            return myProjectionTy;
        }

        @Nonnull
        public Ty getTy() {
            return myTy;
        }

        @Override
        @Nonnull
        public Projection superFoldWith(@Nonnull TypeFolder folder) {
            return new Projection((TyProjection) myProjectionTy.foldWith(folder), myTy.foldWith(folder));
        }

        @Override
        public boolean superVisitWith(@Nonnull TypeVisitor visitor) {
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
        @Nonnull
        private final Ty myTy1;
        @Nonnull
        private final Ty myTy2;

        public Equate(@Nonnull Ty ty1, @Nonnull Ty ty2) {
            myTy1 = ty1;
            myTy2 = ty2;
        }

        @Nonnull
        public Ty getTy1() {
            return myTy1;
        }

        @Nonnull
        public Ty getTy2() {
            return myTy2;
        }

        @Override
        @Nonnull
        public Predicate superFoldWith(@Nonnull TypeFolder folder) {
            return new Equate(myTy1.foldWith(folder), myTy2.foldWith(folder));
        }

        @Override
        public boolean superVisitWith(@Nonnull TypeVisitor visitor) {
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
