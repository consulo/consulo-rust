/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.lang.core.psi.ext;

import consulo.language.psi.PsiElement;
import jakarta.annotation.Nonnull;
import org.rust.lang.core.psi.RsPat;
import org.rust.lang.core.psi.RsPatBinding;

/**
 * Sealed class hierarchy for pattern field kinds.
 */
public abstract class RsPatFieldKind {

    private RsPatFieldKind() {
    }

    /**
     * struct S { a: i32 }
     * let S { a : ref b } = ...
     */
    public static final class Full extends RsPatFieldKind {
        @Nonnull private final PsiElement ident;
        @Nonnull private final RsPat pat;

        public Full(@Nonnull PsiElement ident, @Nonnull RsPat pat) {
            this.ident = ident;
            this.pat = pat;
        }

        @Nonnull
        public PsiElement getIdent() {
            return ident;
        }

        @Nonnull
        public RsPat getPat() {
            return pat;
        }

        @Nonnull
        public String getFieldName() {
            return ident.getText();
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (!(o instanceof Full)) return false;
            Full full = (Full) o;
            return ident.equals(full.ident) && pat.equals(full.pat);
        }

        @Override
        public int hashCode() {
            int result = ident.hashCode();
            result = 31 * result + pat.hashCode();
            return result;
        }
    }

    /**
     * struct S { a: i32 }
     * let S { ref a } = ...
     */
    public static final class Shorthand extends RsPatFieldKind {
        @Nonnull private final RsPatBinding binding;
        private final boolean isBox;

        public Shorthand(@Nonnull RsPatBinding binding, boolean isBox) {
            this.binding = binding;
            this.isBox = isBox;
        }

        @Nonnull
        public RsPatBinding getBinding() {
            return binding;
        }

        public boolean isBox() {
            return isBox;
        }

        @Nonnull
        public String getFieldName() {
            return binding.getName();
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (!(o instanceof Shorthand)) return false;
            Shorthand shorthand = (Shorthand) o;
            return isBox == shorthand.isBox && binding.equals(shorthand.binding);
        }

        @Override
        public int hashCode() {
            int result = binding.hashCode();
            result = 31 * result + (isBox ? 1 : 0);
            return result;
        }
    }
}
