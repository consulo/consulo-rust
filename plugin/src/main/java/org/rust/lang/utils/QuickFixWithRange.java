/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.lang.utils;

import consulo.language.editor.inspection.LocalQuickFix;
import consulo.document.util.TextRange;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

import java.util.Objects;

public class QuickFixWithRange {
    @Nonnull
    private final LocalQuickFix myFix;
    @Nullable
    private final TextRange myAvailabilityRange;

    public QuickFixWithRange(@Nonnull LocalQuickFix fix, @Nullable TextRange availabilityRange) {
        myFix = fix;
        myAvailabilityRange = availabilityRange;
    }

    @Nonnull
    public LocalQuickFix getFix() {
        return myFix;
    }

    @Nullable
    public TextRange getAvailabilityRange() {
        return myAvailabilityRange;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        QuickFixWithRange that = (QuickFixWithRange) o;
        return Objects.equals(myFix, that.myFix) && Objects.equals(myAvailabilityRange, that.myAvailabilityRange);
    }

    @Override
    public int hashCode() {
        return Objects.hash(myFix, myAvailabilityRange);
    }

    @Override
    public String toString() {
        return "QuickFixWithRange(fix=" + myFix + ", availabilityRange=" + myAvailabilityRange + ")";
    }
}
