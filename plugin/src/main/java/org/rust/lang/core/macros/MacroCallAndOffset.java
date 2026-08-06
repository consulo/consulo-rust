/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.lang.core.macros;

import jakarta.annotation.Nonnull;
import org.rust.lang.core.psi.ext.RsPossibleMacroCall;

import java.util.Objects;

public final class MacroCallAndOffset {
    private final RsPossibleMacroCall myCall;
    private final int myAbsoluteOffset;

    public MacroCallAndOffset(@Nonnull RsPossibleMacroCall call, int absoluteOffset) {
        myCall = call;
        myAbsoluteOffset = absoluteOffset;
    }

    @Nonnull
    public RsPossibleMacroCall getCall() {
        return myCall;
    }

    public int getAbsoluteOffset() {
        return myAbsoluteOffset;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        MacroCallAndOffset that = (MacroCallAndOffset) o;
        return myAbsoluteOffset == that.myAbsoluteOffset && myCall.equals(that.myCall);
    }

    @Override
    public int hashCode() {
        return Objects.hash(myCall, myAbsoluteOffset);
    }

    @Override
    public String toString() {
        return "MacroCallAndOffset(call=" + myCall + ", absoluteOffset=" + myAbsoluteOffset + ")";
    }
}
