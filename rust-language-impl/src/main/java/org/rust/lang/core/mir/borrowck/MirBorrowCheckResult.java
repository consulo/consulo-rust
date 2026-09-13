/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.lang.core.mir.borrowck;

import jakarta.annotation.Nonnull;
import org.rust.lang.core.psi.ext.RsElement;

import java.util.List;
import java.util.Objects;

public final class MirBorrowCheckResult {
    @Nonnull
    private final List<RsElement> usesOfUninitializedVariable;
    @Nonnull
    private final List<RsElement> usesOfMovedValue;
    @Nonnull
    private final List<RsElement> moveOutWhileBorrowedValues;

    public MirBorrowCheckResult(
        @Nonnull List<RsElement> usesOfUninitializedVariable,
        @Nonnull List<RsElement> usesOfMovedValue,
        @Nonnull List<RsElement> moveOutWhileBorrowedValues
    ) {
        this.usesOfUninitializedVariable = usesOfUninitializedVariable;
        this.usesOfMovedValue = usesOfMovedValue;
        this.moveOutWhileBorrowedValues = moveOutWhileBorrowedValues;
    }

    @Nonnull
    public List<RsElement> getUsesOfUninitializedVariable() {
        return usesOfUninitializedVariable;
    }

    @Nonnull
    public List<RsElement> getUsesOfMovedValue() {
        return usesOfMovedValue;
    }

    @Nonnull
    public List<RsElement> getMoveOutWhileBorrowedValues() {
        return moveOutWhileBorrowedValues;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        MirBorrowCheckResult that = (MirBorrowCheckResult) o;
        return Objects.equals(usesOfUninitializedVariable, that.usesOfUninitializedVariable)
            && Objects.equals(usesOfMovedValue, that.usesOfMovedValue)
            && Objects.equals(moveOutWhileBorrowedValues, that.moveOutWhileBorrowedValues);
    }

    @Override
    public int hashCode() {
        return Objects.hash(usesOfUninitializedVariable, usesOfMovedValue, moveOutWhileBorrowedValues);
    }

    @Override
    public String toString() {
        return "MirBorrowCheckResult("
            + "usesOfUninitializedVariable=" + usesOfUninitializedVariable
            + ", usesOfMovedValue=" + usesOfMovedValue
            + ", moveOutWhileBorrowedValues=" + moveOutWhileBorrowedValues
            + ")";
    }
}
