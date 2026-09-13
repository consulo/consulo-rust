/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.lang.core.types.ty;

public enum Mutability {
    MUTABLE,
    IMMUTABLE;

    public boolean isMut() {
        return this == MUTABLE;
    }

    public static Mutability valueOf(boolean mutable) {
        return mutable ? MUTABLE : IMMUTABLE;
    }

    public static final Mutability DEFAULT_MUTABILITY = MUTABLE;
}
