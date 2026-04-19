/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.lang.core.types.regions;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.rust.lang.core.psi.RsExpr;

import java.util.Objects;

public abstract class RvalueCandidateType {
    @NotNull
    public abstract RsExpr getTarget();

    @Nullable
    public abstract Scope getLifetime();

    public static class Borrow extends RvalueCandidateType {
        private final RsExpr myTarget;
        @Nullable
        private final Scope myLifetime;

        public Borrow(@NotNull RsExpr target, @Nullable Scope lifetime) {
            myTarget = target;
            myLifetime = lifetime;
        }

        @NotNull
        @Override
        public RsExpr getTarget() {
            return myTarget;
        }

        @Nullable
        @Override
        public Scope getLifetime() {
            return myLifetime;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            Borrow borrow = (Borrow) o;
            return Objects.equals(myTarget, borrow.myTarget) && Objects.equals(myLifetime, borrow.myLifetime);
        }

        @Override
        public int hashCode() {
            return Objects.hash(myTarget, myLifetime);
        }
    }

    public static class Pattern extends RvalueCandidateType {
        private final RsExpr myTarget;
        @Nullable
        private final Scope myLifetime;

        public Pattern(@NotNull RsExpr target, @Nullable Scope lifetime) {
            myTarget = target;
            myLifetime = lifetime;
        }

        @NotNull
        @Override
        public RsExpr getTarget() {
            return myTarget;
        }

        @Nullable
        @Override
        public Scope getLifetime() {
            return myLifetime;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            Pattern pattern = (Pattern) o;
            return Objects.equals(myTarget, pattern.myTarget) && Objects.equals(myLifetime, pattern.myLifetime);
        }

        @Override
        public int hashCode() {
            return Objects.hash(myTarget, myLifetime);
        }
    }
}
