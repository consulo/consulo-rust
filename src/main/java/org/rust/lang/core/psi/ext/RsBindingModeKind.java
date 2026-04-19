/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.lang.core.psi.ext;

import org.jetbrains.annotations.NotNull;
import org.rust.lang.core.types.ty.Mutability;

public abstract class RsBindingModeKind {
    private RsBindingModeKind() {}

    public static final class BindByReference extends RsBindingModeKind {
        @NotNull private final Mutability mutability;

        public BindByReference(@NotNull Mutability mutability) {
            this.mutability = mutability;
        }

        @NotNull
        public Mutability getMutability() {
            return mutability;
        }
    }

    public static final class BindByValue extends RsBindingModeKind {
        @NotNull private final Mutability mutability;

        public BindByValue(@NotNull Mutability mutability) {
            this.mutability = mutability;
        }

        @NotNull
        public Mutability getMutability() {
            return mutability;
        }
    }
}
