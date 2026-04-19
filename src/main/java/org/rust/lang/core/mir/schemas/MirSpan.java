/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.lang.core.mir.schemas;

import com.intellij.psi.PsiElement;
import org.jetbrains.annotations.NotNull;

import java.util.Objects;

public abstract class MirSpan {
    private MirSpan() {
    }

    @NotNull
    public MirSpan getEnd() {
        return new End(getReference());
    }

    @NotNull
    public MirSpan getEndPoint() {
        return new EndPoint(getReference());
    }

    @NotNull
    public abstract PsiElement getReference();

    public static final class Full extends MirSpan {
        @NotNull
        private final PsiElement reference;

        public Full(@NotNull PsiElement reference) {
            this.reference = reference;
        }

        @Override
        @NotNull
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
        @NotNull
        private final PsiElement reference;

        public Start(@NotNull PsiElement reference) {
            this.reference = reference;
        }

        @Override
        @NotNull
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
        @NotNull
        private final PsiElement reference;

        public EndPoint(@NotNull PsiElement reference) {
            this.reference = reference;
        }

        @Override
        @NotNull
        public MirSpan getEndPoint() {
            return this;
        }

        @Override
        @NotNull
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
        @NotNull
        private final PsiElement reference;

        public End(@NotNull PsiElement reference) {
            this.reference = reference;
        }

        @Override
        @NotNull
        public MirSpan getEnd() {
            return this;
        }

        @Override
        @NotNull
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
        @NotNull
        public MirSpan getEnd() {
            return this;
        }

        @Override
        @NotNull
        public MirSpan getEndPoint() {
            return this;
        }

        @Override
        @NotNull
        public PsiElement getReference() {
            throw new IllegalStateException("Fake span have no reference");
        }

        @Override
        public String toString() {
            return "Fake";
        }
    }
}
