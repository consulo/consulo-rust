/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.lang.core.types.infer;

import jakarta.annotation.Nonnull;

import java.util.Objects;

public class Obligation implements TypeFoldable<Obligation> {
    private final int myRecursionDepth;
    @Nonnull
    private Predicate myPredicate;

    public Obligation(@Nonnull Predicate predicate) {
        this(0, predicate);
    }

    public Obligation(int recursionDepth, @Nonnull Predicate predicate) {
        myRecursionDepth = recursionDepth;
        myPredicate = predicate;
    }

    public int getRecursionDepth() {
        return myRecursionDepth;
    }

    @Nonnull
    public Predicate getPredicate() {
        return myPredicate;
    }

    public void setPredicate(@Nonnull Predicate predicate) {
        myPredicate = predicate;
    }

    @Override
    @Nonnull
    public Obligation superFoldWith(@Nonnull TypeFolder folder) {
        return new Obligation(myRecursionDepth, myPredicate.foldWith(folder));
    }

    @Override
    public boolean superVisitWith(@Nonnull TypeVisitor visitor) {
        return myPredicate.visitWith(visitor);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Obligation that = (Obligation) o;
        return myRecursionDepth == that.myRecursionDepth && Objects.equals(myPredicate, that.myPredicate);
    }

    @Override
    public int hashCode() {
        return Objects.hash(myRecursionDepth, myPredicate);
    }
}
