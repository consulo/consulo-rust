/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.lang.core.psi.ext.impl;

import jakarta.annotation.Nonnull;
import org.rust.lang.core.types.ty.Mutability;
import org.rust.lang.core.psi.ext.*;

public abstract class RsBindingModeKind {
    private RsBindingModeKind() {}

    public static final class BindByReference extends RsBindingModeKind {
        @Nonnull private final Mutability mutability;

        public BindByReference(@Nonnull Mutability mutability) {
            this.mutability = mutability;
        }

        @Nonnull
        public Mutability getMutability() {
            return mutability;
        }
    }

    public static final class BindByValue extends RsBindingModeKind {
        @Nonnull private final Mutability mutability;

        public BindByValue(@Nonnull Mutability mutability) {
            this.mutability = mutability;
        }

        @Nonnull
        public Mutability getMutability() {
            return mutability;
        }
    }
}
