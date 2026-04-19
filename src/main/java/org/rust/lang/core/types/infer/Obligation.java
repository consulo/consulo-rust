/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.lang.core.types.infer;

import org.jetbrains.annotations.NotNull;

import java.util.Objects;

public class Obligation implements TypeFoldable<Obligation> {
    private final int myRecursionDepth;
    @NotNull
    private Predicate myPredicate;

    public Obligation(@NotNull Predicate predicate) {
        this(0, predicate);
    }

    public Obligation(int recursionDepth, @NotNull Predicate predicate) {
        myRecursionDepth = recursionDepth;
        myPredicate = predicate;
    }

    public int getRecursionDepth() {
        return myRecursionDepth;
    }

    @NotNull
    public Predicate getPredicate() {
        return myPredicate;
    }

    public void setPredicate(@NotNull Predicate predicate) {
        myPredicate = predicate;
    }

    @Override
    @NotNull
    public Obligation superFoldWith(@NotNull TypeFolder folder) {
        return new Obligation(myRecursionDepth, myPredicate.foldWith(folder));
    }

    @Override
    public boolean superVisitWith(@NotNull TypeVisitor visitor) {
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
