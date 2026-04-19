/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.lang.core.types.infer;

import org.jetbrains.annotations.NotNull;
import org.rust.lang.core.types.ty.Ty;

import java.util.Collections;
import java.util.List;

public class PendingPredicateObligation {
    @NotNull
    private final Obligation myObligation;
    @NotNull
    private List<Ty> myStalledOn;

    public PendingPredicateObligation(@NotNull Obligation obligation) {
        this(obligation, Collections.emptyList());
    }

    public PendingPredicateObligation(@NotNull Obligation obligation, @NotNull List<Ty> stalledOn) {
        myObligation = obligation;
        myStalledOn = stalledOn;
    }

    @NotNull
    public Obligation getObligation() {
        return myObligation;
    }

    @NotNull
    public List<Ty> getStalledOn() {
        return myStalledOn;
    }

    public void setStalledOn(@NotNull List<Ty> stalledOn) {
        myStalledOn = stalledOn;
    }
}
