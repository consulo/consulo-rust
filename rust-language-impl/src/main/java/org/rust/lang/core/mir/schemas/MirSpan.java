/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.lang.core.mir.schemas;

import consulo.language.psi.PsiElement;
import jakarta.annotation.Nonnull;

import java.util.Objects;

public abstract class MirSpan {
    private MirSpan() {
    }

    @Nonnull
    public MirSpan getEnd() {
        return new End(getReference());
    }

    @Nonnull
    public MirSpan getEndPoint() {
        return new EndPoint(getReference());
    }

    @Nonnull
    public abstract PsiElement getReference();

    public static final class Full extends MirSpan {
        @Nonnull
        private final PsiElement reference;

        public Full(@Nonnull PsiElement reference) {
            this.reference = reference;
        }

        @Override
        @Nonnull
        public PsiElement getReference() {
            return reference;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            Full full = (Full) o;
            return Objects.equals(reference, full.reference);
        }

        @Override
        public int hashCode() {
            return Objects.hash(reference);
        }

        @Override
        public String toString() {
            return "Full(reference=" + reference + ")";
        }
    }

    public static final class Start extends MirSpan {
        @Nonnull
        private final PsiElement reference;

        public Start(@Nonnull PsiElement reference) {
            this.reference = reference;
        }

        @Override
        @Nonnull
        public PsiElement getReference() {
            return reference;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            Start start = (Start) o;
            return Objects.equals(reference, start.reference);
        }

        @Override
        public int hashCode() {
            return Objects.hash(reference);
        }

        @Override
        public String toString() {
            return "Start(reference=" + reference + ")";
        }
    }

    public static final class EndPoint extends MirSpan {
        @Nonnull
        private final PsiElement reference;

        public EndPoint(@Nonnull PsiElement reference) {
            this.reference = reference;
        }

        @Override
        @Nonnull
        public MirSpan getEndPoint() {
            return this;
        }

        @Override
        @Nonnull
        public PsiElement getReference() {
            return reference;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            EndPoint endPoint = (EndPoint) o;
            return Objects.equals(reference, endPoint.reference);
        }

        @Override
        public int hashCode() {
            return Objects.hash(reference);
        }

        @Override
        public String toString() {
            return "EndPoint(reference=" + reference + ")";
        }
    }

    public static final class End extends MirSpan {
        @Nonnull
        private final PsiElement reference;

        public End(@Nonnull PsiElement reference) {
            this.reference = reference;
        }

        @Override
        @Nonnull
        public MirSpan getEnd() {
            return this;
        }

        @Override
        @Nonnull
        public PsiElement getReference() {
            return reference;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            End end = (End) o;
            return Objects.equals(reference, end.reference);
        }

        @Override
        public int hashCode() {
            return Objects.hash(reference);
        }

        @Override
        public String toString() {
            return "End(reference=" + reference + ")";
        }
    }

    public static final class Fake extends MirSpan {
        public static final Fake INSTANCE = new Fake();

        private Fake() {
        }

        @Override
        @Nonnull
        public MirSpan getEnd() {
            return this;
        }

        @Override
        @Nonnull
        public MirSpan getEndPoint() {
            return this;
        }

        @Override
        @Nonnull
        public PsiElement getReference() {
            throw new IllegalStateException("Fake span have no reference");
        }

        @Override
        public String toString() {
            return "Fake";
        }
    }
}
