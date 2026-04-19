/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.lang.core.mir.schemas;

import org.jetbrains.annotations.NotNull;
import org.rust.lang.core.thir.ImplicitSelfKind;

import java.util.Objects;

public abstract class MirBindingForm {
    private MirBindingForm() {
    }

    public static final class Var extends MirBindingForm {
        @NotNull
        private final MirVarBindingForm varBinding;

        public Var(@NotNull MirVarBindingForm varBinding) {
            this.varBinding = varBinding;
        }

        @NotNull
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
        @NotNull
        private final ImplicitSelfKind kind;

        public ImplicitSelf(@NotNull ImplicitSelfKind kind) {
            this.kind = kind;
        }

        @NotNull
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
