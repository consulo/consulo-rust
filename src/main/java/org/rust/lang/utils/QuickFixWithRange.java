/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.lang.utils;

import com.intellij.codeInspection.LocalQuickFix;
import com.intellij.openapi.util.TextRange;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Objects;

public class QuickFixWithRange {
    @NotNull
    private final LocalQuickFix myFix;
    @Nullable
    private final TextRange myAvailabilityRange;

    public QuickFixWithRange(@NotNull LocalQuickFix fix, @Nullable TextRange availabilityRange) {
        myFix = fix;
        myAvailabilityRange = availabilityRange;
    }

    @NotNull
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
