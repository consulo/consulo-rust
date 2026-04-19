/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.lang.core.thir;

import org.jetbrains.annotations.NotNull;
import org.rust.lang.core.psi.RsSelfParameter;

public enum ImplicitSelfKind {
    /** Represents a {@code fn x(self)} */
    Immutable,

    /** Represents a {@code fn x(mut self)} */
    Mutable,

    /** Represents a {@code fn x(&self)} */
    ImmutableReference,

    /** Represents a {@code fn x(&mut self)} */
    MutableReference,

    /**
     * Represents when a function does not have a self argument or
     * when a function has a {@code self: X} argument.
     */
    None;

    public boolean hasImplicitSelf() {
        return this != None;
    }

    @NotNull
    public static ImplicitSelfKind from(@NotNull RsSelfParameter self) {
        if (self.getColon() != null) return None;
        boolean isRef = self.getAnd() != null;
        boolean isMut = self.getMut() != null;
        if (isRef) {
            return isMut ? MutableReference : ImmutableReference;
        } else {
            return isMut ? Mutable : Immutable;
        }
    }
}
