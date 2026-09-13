/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.lang.core.mir.schemas;

import jakarta.annotation.Nonnull;
import org.rust.lang.core.thir.ImplicitSelfKind;

import java.util.Objects;

public abstract class MirBindingForm {
    private MirBindingForm() {
    }

    public static final class Var extends MirBindingForm {
        @Nonnull
        private final MirVarBindingForm varBinding;

        public Var(@Nonnull MirVarBindingForm varBinding) {
            this.varBinding = varBinding;
        }

        @Nonnull
        public MirVarBindingForm getVarBinding() {
            return varBinding;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            Var var = (Var) o;
            return Objects.equals(varBinding, var.varBinding);
        }

        @Override
        public int hashCode() {
            return Objects.hash(varBinding);
        }

        @Override
        public String toString() {
            return "Var(varBinding=" + varBinding + ")";
        }
    }

    public static final class ReferenceForGuard extends MirBindingForm {
        public static final ReferenceForGuard INSTANCE = new ReferenceForGuard();

        private ReferenceForGuard() {
        }

        @Override
        public String toString() {
            return "ReferenceForGuard";
        }
    }

    public static final class ImplicitSelf extends MirBindingForm {
        @Nonnull
        private final ImplicitSelfKind kind;

        public ImplicitSelf(@Nonnull ImplicitSelfKind kind) {
            this.kind = kind;
        }

        @Nonnull
        public ImplicitSelfKind getKind() {
            return kind;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            ImplicitSelf that = (ImplicitSelf) o;
            return Objects.equals(kind, that.kind);
        }

        @Override
        public int hashCode() {
            return Objects.hash(kind);
        }

        @Override
        public String toString() {
            return "ImplicitSelf(kind=" + kind + ")";
        }
    }
}
