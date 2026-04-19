/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.lang.core.dfa.borrowck;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.rust.lang.core.dfa.MemoryCategorization;
import org.rust.lang.core.psi.ext.RsElement;

import java.util.Objects;

public abstract class LoanPathElement {
    private LoanPathElement() {
    }

    public static class Deref extends LoanPathElement {
        @NotNull
        public final MemoryCategorization.PointerKind kind;

        public Deref(@NotNull MemoryCategorization.PointerKind kind) {
            this.kind = kind;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (!(o instanceof Deref)) return false;
            return kind.equals(((Deref) o).kind);
        }

        @Override
        public int hashCode() {
            return kind.hashCode();
        }
    }

    public static abstract class Interior extends LoanPathElement {
        @Nullable
        public abstract RsElement getElement();

        public static class Field extends Interior {
            @Nullable
            private final RsElement element;
            @Nullable
            public final String name;

            public Field(@Nullable RsElement element, @Nullable String name) {
                this.element = element;
                this.name = name;
            }

            @Nullable
            @Override
            public RsElement getElement() {
                return element;
            }

            @Override
            public boolean equals(Object o) {
                if (this == o) return true;
                if (!(o instanceof Field)) return false;
                Field f = (Field) o;
                return Objects.equals(element, f.element) && Objects.equals(name, f.name);
            }

            @Override
            public int hashCode() {
                return Objects.hash(element, name);
            }
        }

        public static class Index extends Interior {
            @Nullable
            private final RsElement element;

            public Index(@Nullable RsElement element) {
                this.element = element;
            }

            @Nullable
            @Override
            public RsElement getElement() {
                return element;
            }

            @Override
            public boolean equals(Object o) {
                if (this == o) return true;
                if (!(o instanceof Index)) return false;
                return Objects.equals(element, ((Index) o).element);
            }

            @Override
            public int hashCode() {
                return Objects.hash(element);
            }
        }

        public static class Pattern extends Interior {
            @Nullable
            private final RsElement element;

            public Pattern(@Nullable RsElement element) {
                this.element = element;
            }

            @Nullable
            @Override
            public RsElement getElement() {
                return element;
            }

            @Override
            public boolean equals(Object o) {
                if (this == o) return true;
                if (!(o instanceof Pattern)) return false;
                return Objects.equals(element, ((Pattern) o).element);
            }

            @Override
            public int hashCode() {
                return Objects.hash(element);
            }
        }

        @NotNull
        public static Interior fromCategory(@NotNull MemoryCategorization.Categorization.Interior category, @Nullable RsElement element) {
            if (category instanceof MemoryCategorization.Categorization.Interior.Field) {
                return new Field(element, ((MemoryCategorization.Categorization.Interior.Field) category).name);
            }
            if (category instanceof MemoryCategorization.Categorization.Interior.Index) {
                return new Index(element);
            }
            if (category instanceof MemoryCategorization.Categorization.Interior.Pattern) {
                return new Pattern(element);
            }
            throw new IllegalArgumentException("Unknown Interior category");
        }
    }
}
