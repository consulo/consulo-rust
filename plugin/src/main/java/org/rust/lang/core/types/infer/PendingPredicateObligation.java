/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.lang.core.types.infer;

import jakarta.annotation.Nonnull;
import org.rust.lang.core.types.ty.Ty;

import java.util.Collections;
import java.util.List;

public class PendingPredicateObligation {
    @Nonnull
    private final Obligation myObligation;
    @Nonnull
    private List<Ty> myStalledOn;

    public PendingPredicateObligation(@Nonnull Obligation obligation) {
        this(obligation, Collections.emptyList());
    }

    public PendingPredicateObligation(@Nonnull Obligation obligation, @Nonnull List<Ty> stalledOn) {
        myObligation = obligation;
        myStalledOn = stalledOn;
    }

    @Nonnull
    public Obligation getObligation() {
        return myObligation;
    }

    @Nonnull
    public List<Ty> getStalledOn() {
        return myStalledOn;
    }

    public void setStalledOn(@Nonnull List<Ty> stalledOn) {
        myStalledOn = stalledOn;
    }
}
