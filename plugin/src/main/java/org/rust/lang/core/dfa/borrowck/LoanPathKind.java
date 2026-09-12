/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.lang.core.dfa.borrowck;

import jakarta.annotation.Nonnull;
import org.rust.lang.core.dfa.MemoryCategorization.MutabilityCategory;
import org.rust.lang.core.psi.ext.RsElement;

import java.util.Objects;
import org.rust.lang.core.dfa.MemoryCategorization;

public abstract class LoanPathKind {
    private LoanPathKind() {
    }

    /** Relates to {@link org.rust.lang.core.dfa.MemoryCategorization.Categorization.Local} memory category */
    public static class Var extends LoanPathKind {
        @Nonnull
        public final RsElement declaration;

        public Var(@Nonnull RsElement declaration) {
            this.declaration = declaration;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (!(o instanceof Var)) return false;
            return declaration.equals(((Var) o).declaration);
        }

        @Override
        public int hashCode() {
            return declaration.hashCode();
        }
    }

    /** Relates to {@link org.rust.lang.core.dfa.MemoryCategorization.Categorization.Downcast} memory category */
    public static class Downcast extends LoanPathKind {
        @Nonnull
        public final LoanPath loanPath;
        @Nonnull
        public final RsElement element;

        public Downcast(@Nonnull LoanPath loanPath, @Nonnull RsElement element) {
            this.loanPath = loanPath;
            this.element = element;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (!(o instanceof Downcast)) return false;
            Downcast d = (Downcast) o;
            return loanPath.equals(d.loanPath) && element.equals(d.element);
        }

        @Override
        public int hashCode() {
            return Objects.hash(loanPath, element);
        }
    }

    /** Relates to {@link org.rust.lang.core.dfa.MemoryCategorization.Categorization.Deref} and Interior categories */
    public static class Extend extends LoanPathKind {
        @Nonnull
        public final LoanPath loanPath;
        @Nonnull
        public final MutabilityCategory mutCategory;
        @Nonnull
        public final LoanPathElement lpElement;

        public Extend(@Nonnull LoanPath loanPath, @Nonnull MutabilityCategory mutCategory, @Nonnull LoanPathElement lpElement) {
            this.loanPath = loanPath;
            this.mutCategory = mutCategory;
            this.lpElement = lpElement;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (!(o instanceof Extend)) return false;
            Extend e = (Extend) o;
            return loanPath.equals(e.loanPath) && mutCategory == e.mutCategory && lpElement.equals(e.lpElement);
        }

        @Override
        public int hashCode() {
            return Objects.hash(loanPath, mutCategory, lpElement);
        }
    }
}
