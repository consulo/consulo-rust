/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.lang.core.types.infer;

import org.jetbrains.annotations.NotNull;

import java.util.Collections;
import java.util.List;

public class CoerceOk {
    @NotNull
    private final List<Adjustment> myAdjustments;
    @NotNull
    private final List<Obligation> myObligations;

    public CoerceOk() {
        this(Collections.emptyList(), Collections.emptyList());
    }

    public CoerceOk(@NotNull List<Adjustment> adjustments) {
        this(adjustments, Collections.emptyList());
    }

    public CoerceOk(@NotNull List<Adjustment> adjustments, @NotNull List<Obligation> obligations) {
        myAdjustments = adjustments;
        myObligations = obligations;
    }

    @NotNull
    public List<Adjustment> getAdjustments() {
        return myAdjustments;
    }

    @NotNull
    public List<Obligation> getObligations() {
        return myObligations;
    }
}
