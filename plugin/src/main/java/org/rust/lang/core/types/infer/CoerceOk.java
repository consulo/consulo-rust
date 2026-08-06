/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.lang.core.types.infer;

import jakarta.annotation.Nonnull;

import java.util.Collections;
import java.util.List;

public class CoerceOk {
    @Nonnull
    private final List<Adjustment> myAdjustments;
    @Nonnull
    private final List<Obligation> myObligations;

    public CoerceOk() {
        this(Collections.emptyList(), Collections.emptyList());
    }

    public CoerceOk(@Nonnull List<Adjustment> adjustments) {
        this(adjustments, Collections.emptyList());
    }

    public CoerceOk(@Nonnull List<Adjustment> adjustments, @Nonnull List<Obligation> obligations) {
        myAdjustments = adjustments;
        myObligations = obligations;
    }

    @Nonnull
    public List<Adjustment> getAdjustments() {
        return myAdjustments;
    }

    @Nonnull
    public List<Obligation> getObligations() {
        return myObligations;
    }
}
